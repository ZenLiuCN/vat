package vat.api.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import org.jetbrains.annotations.Nullable;
import vat.api.DomainError;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

///  Pipeline tools
///
/// @author Zen.Liu
/// @since 2025-12-26
@SuppressWarnings("unused")
public sealed interface Pipeline<C, T extends @Nullable Object, S extends Pipeline<C, ?, S>> {
    C context();

    Future<T> get();

    <R> S wrap(Future<R> nextFuture);

    record Simple<C, T>(C context, Future<T> get)
            implements Pipeline<C, T, Simple<C, ?>> {
        @Override
        public <R> Simple<C, R> wrap(Future<R> future) {
            return new Simple<>(context, future);
        }
    }

    /// factory from a value
    static <C, T> Simple<C, T> with(C context, T value) {
        return new Simple<>(context, Future.succeededFuture(value));
    }

    /// factory from a exists future
    static <C, T> Simple<C, T> of(C context, Future<T> value) {
        return new Simple<>(context, value);
    }

    /// map
    default <R extends @Nullable Object> S map(Function<T, R> mapper) {
        return wrap(get().map(mapper));
    }

    /// applicative
    default <R extends @Nullable Object> S then(Function<T, Future<R>> mapper) {
        return wrap(get().flatMap(mapper));
    }

    /// tap no condition
    default S tap(Consumer<T> action) {
        return wrap(get().map(v -> {
            action.accept(v);
            return v;
        }));
    }

    /// tap on condition
    default S tapIf(Predicate<T> predicate, Consumer<T> action) {
        return wrap(get().map(v -> {
            if (predicate.test(v)) action.accept(v);
            return v;
        }));
    }

    /// guard or fail
    default S guard(Predicate<T> predicate, DomainError error) {
        return wrap(get().flatMap(v -> predicate.test(v)
                                          ? Future.succeededFuture(v)
                                          : Future.failedFuture(error)
                                 ));
    }

    /// guard or fail
    default S guard(Predicate<T> predicate, Supplier<DomainError> error) {
        return wrap(get().flatMap(v -> predicate.test(v)
                                          ? Future.succeededFuture(v)
                                          : Future.failedFuture(error.get())
                                 ));
    }

    /// conditional applicative
    default S thenIf(Predicate<T> predicate, Function<T, Future<T>> action) {
        return wrap(get().flatMap(val ->
                                          predicate.test(val) ? action.apply(val) : Future.succeededFuture(val)
                                 ));
    }

    /// two branch
    default <R> S match(
            Predicate<T> predicate,
            Function<T, Future<R>> onTrue,
            Function<T, Future<R>> onFalse) {
        return wrap(get().flatMap(val ->
                                          predicate.test(val) ? onTrue.apply(val) : onFalse.apply(val)
                                 ));
    }

    /// check and execute (not waiting result)
    default S check(Predicate<T> predicate, Function<T, Future<Void>> validation) {
        return wrap(get().flatMap(v -> {
            if (predicate.test(v)) {
                return validation.apply(v).map(ignore -> v);
            }
            return Future.succeededFuture(v);
        }));
    }

    /// recover on spec error
    default <E extends Throwable> S recover(Class<E> errorType, Function<E, T> fallback) {
        return wrap(get().recover(err -> {
            if (errorType.isInstance(err)) {
                return Future.succeededFuture(fallback.apply(errorType.cast(err)));
            }
            return Future.failedFuture(err);
        }));
    }

    /// recover on spec error
    default S recover(Predicate<Throwable> errorPred, Function<Throwable, T> fallback) {
        return wrap(get().recover(err -> {
            if (errorPred.test(err)) {
                return Future.succeededFuture(fallback.apply(err));
            }
            return Future.failedFuture(err);
        }));
    }

    /// resource with cleanup
    default <R extends @Nullable Object> S bracket(Function<T, Future<R>> use, Function<T, Future<Void>> release) {
        return wrap(get().flatMap(resource -> use.apply(resource)
                                                 .eventually(() -> release.apply(resource))));
    }

    /// zip
    default <U extends @Nullable Object, R extends @Nullable Object> S zip(Function<T, Future<U>> other,
                                                                           BiFunction<T, U, R> combiner) {
        return wrap(get().flatMap(t ->
                                          Future.all(Future.succeededFuture(t), other.apply(t))
                                                .map(composite -> combiner.apply(t, composite.resultAt(1)))
                                 ));
    }

    /// zip with other pipe
    default <U, R> S zipPar(Pipeline<C, U, ?> other, BiFunction<T, U, R> combiner) {
        return wrap(Future.all(this.get(), other.get())
                          .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1))));
    }

    /// race with functions
    @SuppressWarnings("unchecked")
    default S race(Function<T, Future<T>>... competitors) {
        return wrap(Future.any(Arrays.stream(competitors)
                                     .map(c -> get().flatMap(c))
                                     .toList())
                          .map(cf -> (T) cf.result()));
    }

    /// race with another pipe
    @SuppressWarnings("unchecked")
    default S raceWith(Pipeline<C, T, ?> other) {
        return wrap(Future.any(this.get(), other.get()).map(cf -> (T) cf.resultAt(0)));
    }

    /// tap the error
    default S tapError(Handler<Throwable> handler) {
        return wrap(get().onFailure(handler));
    }

    /// replace to const value
    default <R> S as(R value) {
        return wrap(get().map(v -> value));
    }

    /// with timeout
    default S timeout(long amount, TimeUnit unit) {
        return wrap(get().timeout(amount, unit));
    }

    /// blocking mapping
    default <R extends @Nullable Object> S blocking(Vertx vertx, Function<T, R> blockingHandler) {
        return wrap(get().flatMap(t -> vertx.executeBlocking(() -> blockingHandler.apply(t))));
    }

    /// check value
    default S expecting(Expectation<? super T> action) {
        return wrap(get().expecting(action));
    }

    /// convert error and result to R
    default <R> S fold(Function<Throwable, R> onFailure, Function<T, R> onSuccess) {
        return wrap(get()
                            .map(onSuccess)
                            .recover(err -> Future.succeededFuture(onFailure.apply(err))));
    }

    /// same as `Future.eventually``
    default S ensuring(Supplier<Future<Void>> action) {
        return wrap(get().eventually(action));
    }

    /// sticky to the vertx context
    default S sticky(Vertx vertx) {
        var targetContext = vertx.getOrCreateContext();
        return wrap(get().flatMap(v -> {
            Promise<T> promise = Promise.promise();
            targetContext.runOnContext(x -> promise.complete(v));
            return promise.future();
        }));
    }

    /// retryable
    default <R extends @Nullable Object> S retry(Vertx vertx, RetryPolicy policy,
                                                 Function<T, Future<R>> action) {
        return wrap(get().flatMap(
                val -> executeRetry(vertx, policy, () -> action.apply(val), 1, policy.initialDelayMs())));
    }


    private static <R extends @Nullable Object> Future<R> executeRetry(
            Vertx vertx,
            RetryPolicy policy,
            Supplier<Future<R>> operation,
            int attempt,
            long nextDelay) {
        return operation.get().recover(err -> {
            if (attempt > policy.maxRetries() || !policy.retryOn().test(err)) {
                return Future.failedFuture(err);
            }
            long sleepTime = ThreadLocalRandom.current().nextLong(0, nextDelay);
            Promise<R> promise = Promise.promise();
            vertx.setTimer(sleepTime, id -> {
                long increasedDelay = Math.min(nextDelay * 2, policy.maxDelayMs());
                executeRetry(vertx, policy, operation, attempt + 1, increasedDelay).onComplete(promise);
            });
            return promise.future();
        });
    }

    record RetryPolicy(
            int maxRetries,
            long initialDelayMs,
            long maxDelayMs,
            Predicate<Throwable> retryOn
    ) {
        public static RetryPolicy fast() {
            return new RetryPolicy(3, 100, 1000, err -> true);
        }

        public static RetryPolicy network() {
            return new RetryPolicy(5, 200, 10000,
                                   t -> t instanceof java.io.IOException || t.getMessage().contains("timeout"));
        }
    }


    default <NC> Pipeline<NC, T, ?> withContext(Function<NC, C> contextAdapter, NC newContext) {
        return Pipeline.of(newContext, get());
    }

    /// Pipeline with Resilience breaker
    ///
    /// @apiNote io.vert:vert-CircuitBreaker required
    sealed interface Resilience<C, T extends @Nullable Object> extends Pipeline<C, T, Resilience<C, ?>> {
        @Override
        <R> Resilience<C, R> wrap(Future<R> future);

        static <C, T> resilience<C, T> with(C context, T value) {
            return new resilience<>(context, Future.succeededFuture(value));
        }

        static <C, T> resilience<C, T> of(C context, Future<T> value) {
            return new resilience<>(context, value);
        }

        record resilience<C, T extends @Nullable Object>(C context, Future<T> get)
                implements Resilience<C, T> {
            @Override
            public <R> resilience<C, R> wrap(Future<R> nextFuture) {
                return new resilience<>(context, nextFuture);
            }
        }

        @Override
        default <NC> Resilience<NC, T> withContext(Function<NC, C> contextAdapter, NC newContext) {
            return Resilience.of(newContext, get());
        }

        /// zip with another Resilience pipe
        default <U, R> Resilience<C, R> zipPar(Resilience<C, U> other, BiFunction<T, U, R> combiner) {
            return wrap(Future.all(this.get(), other.get())
                              .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1))));
        }

        default <R extends @Nullable Object> Resilience<C, R> breaker(CircuitBreaker breaker,
                                                                      Function<T, Future<R>> action) {
            return wrap(get().flatMap(val -> breaker.execute(p -> action.apply(val).onComplete(p))));
        }
    }

    sealed interface Batch<C, I, T extends Iterable<I>> extends Pipeline<C, T, Batch<C, I, ? extends Iterable<I>>> {

        static <C, I, T extends Iterable<I>> batch<C, I, T> of(C context, Future<T> value) {
            return new batch<>(context, value);
        }

        record batch<C, I, T extends Iterable<I>>(C context, Future<T> get)
                implements Batch<C, I, T> {
            @Override
            @SuppressWarnings("unchecked")
            public <R> Batch<C, I, ? extends Iterable<I>> wrap(Future<R> nextFuture) {
                return new batch<>(context, (Future<Iterable<I>>) nextFuture);
            }
        }

        default <R extends @Nullable Object> Batch<C, R, List<R>> traverse(Function<I, R> predicate) {
            return Batch.of(context(), get().map(
                    i -> i == null ? null : StreamSupport.stream(i.spliterator(), false).map(predicate).toList()));
        }

        default <R extends @Nullable Object> Batch<C, R, List<R>> traversePar(Function<I, Future<R>> mapper) {
            return new batch<>(context(), get().flatMap(items -> {
                if (items == null) return Future.succeededFuture(List.of());
                var futures = StreamSupport.stream(items.spliterator(), false)
                                           .map(mapper)
                                           .toList();
                return Future.all(futures).map(CompositeFuture::list);
            }));
        }

        default <R extends @Nullable Object> Batch<C, R, List<R>> traverseSeq(Function<I, Future<R>> mapper) {
            return new batch<>(context(), get().flatMap(items -> {
                if (items == null) return Future.succeededFuture(new ArrayList<>());
                return StreamSupport.stream(items.spliterator(), false)
                                    .reduce(
                                            Future.succeededFuture(new ArrayList<R>()),
                                            (future, item) -> future.flatMap(list ->
                                                                                     mapper.apply(item).map(res -> {
                                                                                         list.add(res);
                                                                                         return list;
                                                                                     })
                                                                            ),
                                            (f1, f2) -> f1 // Combiner (not used in sequential)
                                           );
            }));
        }

        default <R extends @Nullable Object> Batch<C, R, List<R>> traversePar(int concurrency,
                                                                              Function<I, Future<R>> mapper) {
            if (concurrency <= 0) return traversePar(mapper);
            if (concurrency == 1) return traverseSeq(mapper);
            return new batch<>(context(), get().flatMap(items -> {
                if (items == null) return Future.succeededFuture(new ArrayList<>());
                List<I> itemList = StreamSupport.stream(items.spliterator(), false).toList();
                if (itemList.isEmpty()) return Future.succeededFuture(new ArrayList<>());
                Promise<List<R>> promise = Promise.promise();
                List<R> results = new java.util.concurrent.CopyOnWriteArrayList<>();
                java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicInteger active = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
                Runnable next = new Runnable() {
                    @Override
                    public void run() {
                        if (error.get() != null) return;
                        int i = index.getAndIncrement();
                        if (i >= itemList.size()) {
                            if (active.get() == 0 && !promise.future().isComplete()) {
                                // Sort results if order is required, or return as is
                                promise.complete(new ArrayList<>(results));
                            }
                            return;
                        }
                        active.incrementAndGet();
                        mapper.apply(itemList.get(i)).onComplete(ar -> {
                            active.decrementAndGet();
                            if (ar.succeeded()) {
                                results.add(ar.result());
                                run(); // Process next item
                            } else {
                                if (error.compareAndSet(null, ar.cause())) {
                                    promise.fail(ar.cause());
                                }
                            }
                        });
                    }
                };
                // Fill the "pipe" up to the concurrency limit
                for (int j = 0; j < Math.min(concurrency, itemList.size()); j++) {
                    next.run();
                }
                return promise.future();
            }));
        }

        default Batch<C, I, List<I>> filterPar(Function<I, Future<Boolean>> predicate) {
            Future<List<I>> filtered = get().flatMap(items -> {
                if (items == null) return Future.succeededFuture(List.of());
                return Future.all(StreamSupport.stream(items.spliterator(), false)
                                               .map(i -> predicate.apply(i).map(match -> new Pair<>(i, match)))
                                               .toList()
                                 ).map(cf -> cf.<Pair<I, Boolean>>list().stream()
                                               .filter(Pair::value)
                                               .map(Pair::key)
                                               .toList());
            });
            return Batch.of(context(), filtered);
        }

        default Batch<C, I, List<I>> filter(Predicate<I> predicate) {
            return Batch.of(context(), get().map(
                    i -> i == null ? null : StreamSupport.stream(i.spliterator(), false).filter(predicate).toList()));
        }


        default <K> Simple<C, Map<K, List<I>>> groupBy(Function<I, K> classifier) {
            return new Simple<>(context(), get().map(items ->
                                                             items == null ? new HashMap<>() :
                                                                     StreamSupport.stream(items.spliterator(), false)
                                                                                  .collect(
                                                                                          Collectors.groupingBy(
                                                                                                  classifier))
                                                    ));
        }

        default <K, V> Simple<C, Map<K, List<V>>> groupBy(
                Function<I, K> classifier,
                Function<I, V> valueMapper) {
            return new Simple<>(context(), get().map(items ->
                                                             items == null ? new HashMap<>() :
                                                                     StreamSupport.stream(items.spliterator(), false)
                                                                                  .collect(
                                                                                          Collectors.groupingBy(
                                                                                                  classifier,
                                                                                                  Collectors.mapping(
                                                                                                          valueMapper,
                                                                                                          Collectors.toList())
                                                                                                               ))
                                                    ));
        }

        default <R> Simple<C, R> reduce(R identity, BiFunction<R, I, R> accumulator) {
            return new Simple<>(context(), get().map(items ->
                                                             items == null ? identity :
                                                                     StreamSupport.stream(items.spliterator(), false)
                                                                                  .reduce(identity, accumulator,
                                                                                          (a, b) -> a)
                                                    ));
        }

        record Pair<K, V>(K key, V value) {}
    }

    /// convert to a Batch pipe
    default <I> Batch<C, I, Iterable<I>> batch(Function<T, Iterable<I>> map) {
        return Batch.of(context(), get().map(map));
    }

}
