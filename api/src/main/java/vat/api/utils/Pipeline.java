package vat.api.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import vat.api.DomainError;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

///  Pipeline tools
///
/// @author Zen.Liu
/// @since 2025-12-26
@SuppressWarnings("unused")
@NullMarked
public sealed interface Pipeline<C, T, S extends Pipeline<C, ?, S>> {
    C context();

    Future<T> get();

    <R> Pipeline<C, R, S> wrap(Future<R> nextFuture);

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
    default <R> Pipeline<C, R, S> map(Function<T, R> mapper) {
        return wrap(get().map(mapper));
    }

    default <R> Pipeline<C, Optional<R>, S> mapOpt(Function<T, @Nullable R> mapper) {
        return wrap(get().map(mapper).map(Optional::ofNullable));
    }

    default <R> Pipeline<C, R, S> mapCtx(BiFunction<C, T, R> mapper) {
        return map(i -> mapper.apply(context(), i));
    }

    /// applicative

    default <R> Pipeline<C, R, S> flatMap(Function<T, Future<R>> mapper) {
        return wrap(get().flatMap(mapper));
    }

    default <R> Pipeline<C, Optional<R>, S> flatMapOpt(Function<T, Future<@Nullable R>> mapper) {
        return wrap(get().flatMap(mapper).map(Optional::ofNullable));
    }

    default <R> Pipeline<C, R, S> flatMapCtx(BiFunction<C, T, Future<R>> mapper) {
        return flatMap(i -> mapper.apply(context(), i));
    }

    default Pipeline<C, T, S> recover(Function<Throwable, T> fallback) {
        return wrap(get().recover(err -> Future.succeededFuture(fallback.apply(err))));
    }

    default Pipeline<C, T, S> flatRecover(Function<Throwable, Future<T>> fallback) {
        return wrap(get().recover(fallback));
    }

    /// tap no condition
    default Pipeline<C, T, S> peek(Consumer<T> action) {
        return map(v -> {
            action.accept(v);
            return v;
        });
    }

    default Pipeline<C, T, S> peekCtx(BiConsumer<C, T> action) {
        return map(v -> {
            action.accept(context(), v);
            return v;
        });
    }

    /// tap on condition
    default Pipeline<C, T, S> peekIf(Predicate<T> predicate, Consumer<T> action) {
        return map(v -> {
            if (predicate.test(v)) action.accept(v);
            return v;
        });
    }

    default Pipeline<C, T, S> peekIfCtx(BiPredicate<C, T> predicate, BiConsumer<C, T> action) {
        return map(v -> {
            if (predicate.test(context(), v)) action.accept(context(), v);
            return v;
        });
    }

    /// guard or fail
    default Pipeline<C, T, S> guard(Predicate<T> predicate, DomainError error) {
        return flatMap(v -> predicate.test(v)
                               ? Future.succeededFuture(v)
                               : Future.failedFuture(error)
                      );
    }

    default Pipeline<C, T, S> guardCtx(BiPredicate<C, T> predicate, DomainError error) {
        return flatMap(v -> predicate.test(context(), v)
                               ? Future.succeededFuture(v)
                               : Future.failedFuture(error)
                      );
    }

    /// guard or fail
    default Pipeline<C, T, S> guard(Predicate<T> predicate, Supplier<DomainError> error) {
        return flatMap(v -> predicate.test(v)
                               ? Future.succeededFuture(v)
                               : Future.failedFuture(error.get())
                      );
    }

    default Pipeline<C, T, S> guardCtx(BiPredicate<C, T> predicate, Function<C, DomainError> error) {
        return flatMap(v -> predicate.test(context(), v)
                               ? Future.succeededFuture(v)
                               : Future.failedFuture(error.apply(context()))
                      );
    }

    /// conditional applicative
    default Pipeline<C, T, S> flatMapIf(Predicate<T> predicate, Function<T, Future<T>> action) {
        return flatMap(val -> predicate.test(val)
                               ? action.apply(val)
                               : Future.succeededFuture(val)
                      );
    }

    default Pipeline<C, T, S> flatMapIfCtx(BiPredicate<C, T> predicate, BiFunction<C, T, Future<T>> action) {
        return flatMap(val -> predicate.test(context(), val)
                               ? action.apply(context(), val)
                               : Future.succeededFuture(
                               val)
                      );
    }

    /// two branch
    default <R> Pipeline<C, R, S> match(
            Predicate<T> predicate,
            Function<T, Future<R>> onTrue,
            Function<T, Future<R>> onFalse) {
        return flatMap(val -> predicate.test(val)
                               ? onTrue.apply(val)
                               : onFalse.apply(val)
                      );
    }

    default <R> Pipeline<C, R, S> matchCtx(
            BiPredicate<C, T> predicate,
            BiFunction<C, T, Future<R>> onTrue,
            BiFunction<C, T, Future<R>> onFalse) {
        return flatMap(val -> predicate.test(context(), val)
                               ? onTrue.apply(context(), val)
                               : onFalse.apply(context(), val)
                      );
    }

    /// check and execute (will waiting result)
    default Pipeline<C, T, S> check(Predicate<T> predicate, Function<T, Future<Void>> validation) {
        return flatMap(v -> {
            if (predicate.test(v)) {
                return validation.apply(v).map(ignore -> v);
            }
            return Future.succeededFuture(v);
        });
    }

    default Pipeline<C, T, S> checkCtx(BiPredicate<C, T> predicate, BiFunction<C, T, Future<Void>> validation) {
        return flatMap(v -> {
            if (predicate.test(context(), v)) {
                return validation.apply(context(), v).map(ignore -> v);
            }
            return Future.succeededFuture(v);
        });
    }

    /// recover on spec error
    default <E extends Throwable> Pipeline<C, T, S> recover(Class<E> errorType, Function<E, T> fallback) {
        return flatRecover(err -> {
            if (errorType.isInstance(err)) {
                return Future.succeededFuture(fallback.apply(errorType.cast(err)));
            }
            return Future.failedFuture(err);
        });
    }

    default <E extends Throwable> Pipeline<C, T, S> recoverCtx(Class<E> errorType, BiFunction<C, E, T> fallback) {
        return flatRecover(err -> {
            if (errorType.isInstance(err)) {
                return Future.succeededFuture(fallback.apply(context(), errorType.cast(err)));
            }
            return Future.failedFuture(err);
        });
    }

    /// recover on spec error
    default Pipeline<C, T, S> recover(Predicate<Throwable> errorPred, Function<Throwable, T> fallback) {
        return flatRecover(err -> {
            if (errorPred.test(err)) {
                return Future.succeededFuture(fallback.apply(err));
            }
            return Future.failedFuture(err);
        });
    }

    default Pipeline<C, T, S> recoverCtx(BiPredicate<C, Throwable> errorPred, BiFunction<C, Throwable, T> fallback) {
        return flatRecover(err -> {
            if (errorPred.test(context(), err)) {
                return Future.succeededFuture(fallback.apply(context(), err));
            }
            return Future.failedFuture(err);
        });
    }

    /// resource with cleanup
    default <R> Pipeline<C, R, S> bracket(Function<T, Future<R>> use,
                                          Function<T, Future<Void>> release) {
        return flatMap(resource -> use
                .apply(resource)
                .eventually(() -> release.apply(resource)));
    }

    default <R> Pipeline<C, R, S> bracketCtx(BiFunction<C, T, Future<R>> use,
                                             BiFunction<C, T, Future<Void>> release) {
        return flatMap(resource -> use
                .apply(context(), resource)
                .eventually(() -> release.apply(context(), resource)));
    }

    /// zip
    default <U, R> Pipeline<C, R, S> zip(Function<T, Future<U>> other,
                                         BiFunction<T, U, R> combiner) {
        return flatMap(t -> Future
                               .all(Future.succeededFuture(t), other.apply(t))
                               .map(composite -> combiner.apply(t, composite.resultAt(1)))
                      );
    }

    default <U, R> Pipeline<C, R, S> zipCtx(
            BiFunction<C, T, Future<U>> other,
            BiFunction<T, U, R> combiner) {
        return flatMap(t -> Future
                               .all(Future.succeededFuture(t), other.apply(context(), t))
                               .map(composite -> combiner.apply(t, composite.resultAt(1)))
                      );
    }

    default <U, R> Pipeline<C, R, S> zipCtx(
            BiFunction<C, T, Future<U>> other,
            TriFunction<C, T, U, R> combiner) {
        return flatMap(t -> Future
                               .all(Future.succeededFuture(t), other.apply(context(), t))
                               .map(composite -> combiner.apply(context(), t, composite.resultAt(1)))
                      );
    }

    /// zip with other pipe
    default <U, R> Pipeline<C, R, S> zipPar(Pipeline<C, U, ?> other, BiFunction<T, U, R> combiner) {
        return wrap(Future.all(this.get(), other.get())
                          .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1))));
    }


    /// race with functions
    @SuppressWarnings("unchecked")
    default Pipeline<C, T, S> race(Function<T, Future<T>>... competitors) {
        return wrap(Future.any(Arrays.stream(competitors)
                                     .map(c -> get().flatMap(c))
                                     .toList())
                          .map(cf -> (T) cf.result()));
    }

    @SuppressWarnings("unchecked")
    default Pipeline<C, T, S> raceCtx(BiFunction<C, T, Future<T>>... competitors) {
        return wrap(Future.any(Arrays.stream(competitors)
                                     .map(c -> get().flatMap(i -> c.apply(context(), i)))
                                     .toList())
                          .map(cf -> (T) cf.result()));
    }

    /// race with another pipe
    @SuppressWarnings("unchecked")
    default Pipeline<C, T, S> raceWith(Pipeline<C, T, ?> other) {
        return wrap(Future.any(this.get(), other.get()).map(cf -> (T) cf.resultAt(0)));
    }

    /// tap the error
    default Pipeline<C, T, S> onError(Handler<Throwable> handler) {
        return wrap(get().onFailure(handler));
    }

    default Pipeline<C, T, S> onErrorCtx(BiConsumer<C, Throwable> handler) {
        return wrap(get().onFailure(e -> handler.accept(context(), e)));
    }

    /// replace to const value
    default <R> Pipeline<C, R, S> value(R value) {
        return map(v -> value);
    }

    /// with timeout
    default Pipeline<C, T, S> timeout(long amount, TimeUnit unit) {
        return wrap(get().timeout(amount, unit));
    }

    /// blocking mapping
    default <R> Pipeline<C, R, S> blocking(Vertx vertx, Function<T, R> blockingHandler) {
        return flatMap(t -> vertx.executeBlocking(() -> blockingHandler.apply(t)));
    }

    default <R> Pipeline<C, R, S> blockingCtx(Vertx vertx,
                                              BiFunction<C, T, R> blockingHandler) {
        return flatMap(t -> vertx.executeBlocking(() -> blockingHandler.apply(context(), t)));
    }

    /// check value
    default Pipeline<C, T, S> should(Expectation<? super T> action) {
        return wrap(get().expecting(action));
    }

    /// convert error and result to R
    default <R> Pipeline<C, R, S> fold(Function<Throwable, R> onFailure, Function<T, R> onSuccess) {
        return wrap(get()
                            .map(onSuccess)
                            .recover(err -> Future.succeededFuture(onFailure.apply(err))));
    }

    default <R> Pipeline<C, R, S> foldCtx(BiFunction<C, Throwable, R> onFailure, BiFunction<C, T, R> onSuccess) {
        return wrap(get()
                            .map(i -> onSuccess.apply(context(), i))
                            .recover(err -> Future.succeededFuture(onFailure.apply(context(), err))));
    }

    /// same as `Future.eventually``
    default Pipeline<C, T, S> eventually(Supplier<Future<Void>> action) {
        return wrap(get().eventually(action));
    }

    default Pipeline<C, T, S> eventuallyCtx(Function<C, Future<Void>> action) {
        return wrap(get().eventually(() -> action.apply(context())));
    }

    /// sticky to the vertx context
    default Pipeline<C, T, S> sticky(Vertx vertx) {
        var targetContext = vertx.getOrCreateContext();
        return flatMap(v -> {
            Promise<T> promise = Promise.promise();
            targetContext.runOnContext(x -> promise.complete(v));
            return promise.future();
        });
    }


    default <NC> Pipeline<NC, T, ?> withContext(NC newContext) {
        return Pipeline.of(newContext, get());
    }

    /// Pipeline with Resilience breaker
    ///
    /// @apiNote io.vert:vert-CircuitBreaker required
    sealed interface Resilience<C, T> extends Pipeline<C, T, Resilience<C, ?>> {
        @Override
        <R> Resilience<C, R> wrap(Future<R> future);

        static <C, T> Resilience<C, T> with(C context, T value) {
            return new resilience<>(context, Future.succeededFuture(value));
        }

        static <C, T> Resilience<C, T> of(C context, Future<T> value) {
            return new resilience<>(context, value);
        }

        static <C, T> Resilience<C, T> from(Pipeline<C, T, ?> pipeline) {
            return new resilience<>(pipeline.context(), pipeline.get());
        }

        static <C, I, T extends Iterable<I>> Resilience<C, T> from(Batch<C, I, T> pipeline) {
            return new resilience<>(pipeline.context(), pipeline.get());
        }

        record resilience<C, T>(C context, Future<T> get)
                implements Resilience<C, T> {
            @Override
            public <R> resilience<C, R> wrap(Future<R> nextFuture) {
                return new resilience<>(context, nextFuture);
            }
        }

        @Override
        default <NC> Resilience<NC, T> withContext(NC newContext) {
            return Resilience.of(newContext, get());
        }

        /// zip with another Resilience pipe
        default <U, R> Resilience<C, R> zipPar(Resilience<C, U> other, BiFunction<T, U, R> combiner) {
            return wrap(Future.all(this.get(), other.get())
                              .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1))));
        }

        default <R> Resilience<C, R> withBreaker(CircuitBreaker breaker,
                                                 Function<T, Future<R>> action) {
            return wrap(get().flatMap(val -> breaker.execute(p -> action.apply(val).onComplete(p))));
        }

        default <R> Resilience<C, R> witBreakerCtx(CircuitBreaker breaker,
                                                   BiFunction<C, T, Future<R>> action) {
            return withBreaker(breaker, i -> action.apply(context(), i));
        }

        default <R> Resilience<C, R> breakerCtx(Function<C, CircuitBreaker> breaker,
                                                BiFunction<C, T, Future<R>> action) {
            return wrap(get().flatMap(
                    val -> breaker.apply(context()).execute(p -> action.apply(context(), val).onComplete(p))));
        }
    }

    sealed interface Batch<C, I, T extends Iterable<I>> {
        C context();

        Future<T> get();

        <R, RS extends Iterable<R>> Batch<C, R, RS> wrap(Future<RS> nextFuture);


        static <C, I, T extends Iterable<I>> batch<C, I, T> of(C context, Future<T> value) {
            return new batch<>(context, value);
        }

        static <C, I, T extends Iterable<I>> batch<C, I, T> with(C context, T value) {
            return new batch<>(context, Future.succeededFuture(value));
        }

        record batch<C, I, T extends Iterable<I>>(C context, Future<T> get)
                implements Batch<C, I, T> {

            @Override
            public <R, RS extends Iterable<R>> Batch<C, R, RS> wrap(Future<RS> nextFuture) {
                return new batch<>(context, nextFuture);
            }
        }

        default <R> Batch<C, R, List<R>> mapEach(Function<I, R> mapper) {
            return Batch.of(context(),
                            get().map(i -> StreamSupport.stream(i.spliterator(), false).map(mapper).toList()));
        }

        default <R> Batch<C, R, List<R>> mapEachCtx(BiFunction<C, I, R> mapper) {
            return Batch.of(context(), get().map(i -> StreamSupport.stream(i.spliterator(), false)
                                                                   .map(it -> mapper.apply(context(), it)).toList()));
        }

        default <R> Batch<C, R, List<R>> mapEachPar(Function<I, Future<R>> mapper) {
            return Batch.of(context(),
                            get().flatMap(items -> Future.all(StreamSupport.stream(items.spliterator(), false)
                                                                           .map(mapper)
                                                                           .toList())
                                                         .map(CompositeFuture::list)));
        }

        default <R> Batch<C, R, List<R>> mapEachParCtx(BiFunction<C, I, Future<R>> mapper) {
            return new batch<>(context(),
                               get().flatMap(items -> Future.all(StreamSupport.stream(items.spliterator(), false)
                                                                              .map(it -> mapper.apply(context(), it))
                                                                              .toList())
                                                            .map(CompositeFuture::list)));
        }

        default <R> Batch<C, R, List<R>> mapEachSeq(Function<I, Future<R>> mapper) {
            return new batch<>(context(), get().flatMap(items -> StreamSupport.stream(items.spliterator(), false)
                                                                              .reduce(
                                                                                      Future.succeededFuture(
                                                                                              new ArrayList<>()),
                                                                                      (future, item) -> future.flatMap(
                                                                                              list ->
                                                                                                      mapper.apply(item)
                                                                                                            .map(res -> {
                                                                                                                list.add(
                                                                                                                        res);
                                                                                                                return list;
                                                                                                            })
                                                                                                                      ),
                                                                                      (f1, f2) -> f1
                                                                                      // Combiner (not used in sequential)
                                                                                     )));
        }

        default <R> Batch<C, R, List<R>> mapEachSeqCtx(BiFunction<C, I, Future<R>> mapper) {
            return new batch<>(context(), get().flatMap(items -> StreamSupport.stream(items.spliterator(), false)
                                                                              .reduce(
                                                                                      Future.succeededFuture(
                                                                                              new ArrayList<>()),
                                                                                      (future, item) -> future.flatMap(
                                                                                              list ->
                                                                                                      mapper.apply(
                                                                                                                    context(),
                                                                                                                    item)
                                                                                                            .map(res -> {
                                                                                                                list.add(
                                                                                                                        res);
                                                                                                                return list;
                                                                                                            })
                                                                                                                      ),
                                                                                      (f1, f2) -> f1
                                                                                      // Combiner (not used in sequential)
                                                                                     )));
        }

        default <R> Batch<C, R, List<R>> mapEachPar(int concurrency,
                                                    Function<I, Future<R>> mapper) {
            return new batch<>(context(), get().flatMap(items -> {
                List<I> list = StreamSupport.stream(items.spliterator(), false).toList();
                if (list.isEmpty()) return Future.succeededFuture(List.of());
                record Orchestrator<I, R>(
                        List<I> input,
                        R[] results,
                        AtomicInteger nextIndex,
                        AtomicInteger remaining,
                        Promise<List<R>> promise,
                        Function<I, Future<R>> mapper
                ) {
                    void fillLane() {
                        int i = nextIndex.getAndIncrement();
                        if (i >= input.size() || promise.future().isComplete()) return;

                        mapper.apply(input.get(i)).onComplete(ar -> {
                            if (ar.succeeded()) {
                                results[i] = ar.result();
                                if (remaining.decrementAndGet() == 0) {
                                    promise.tryComplete(Arrays.asList(results));
                                } else {
                                    fillLane(); // Pull next item into this lane
                                }
                            } else {
                                promise.tryFail(ar.cause());
                            }
                        });
                    }
                }
                @SuppressWarnings("unchecked")
                Orchestrator<I, R> controller = new Orchestrator<>(
                        list,
                        (R[]) new Object[list.size()],
                        new AtomicInteger(0),
                        new AtomicInteger(list.size()),
                        Promise.promise(),
                        mapper
                );

                // Start the concurrent lanes
                int lanes = Math.min(concurrency > 0 ? concurrency : list.size(), list.size());
                for (int i = 0; i < lanes; i++) {
                    controller.fillLane();
                }

                return controller.promise().future();
            }));
        }

        default <R> Batch<C, R, List<R>> mapEachParCtx(int limit, BiFunction<C, I, Future<R>> mapper) {
            return mapEachPar(limit, item -> mapper.apply(context(), item));
        }

        default Batch<C, I, List<I>> filterPar(Function<I, Future<Boolean>> predicate) {
            return Batch.of(context(), get().flatMap(items -> {
                record Pair<K, V>(K key, V value) {}
                return Future.all(StreamSupport.stream(items.spliterator(), false)
                                               .map(i -> predicate.apply(i).map(match -> new Pair<>(i, match)))
                                               .toList()
                                 ).map(cf -> cf.<Pair<I, Boolean>>list().stream()
                                               .filter(Pair::value)
                                               .map(Pair::key)
                                               .toList());
            }));
        }

        default Batch<C, I, List<I>> filterParCtx(BiFunction<C, I, Future<Boolean>> predicate) {
            return Batch.of(context(), get().flatMap(items -> {
                record Pair<K, V>(K key, V value) {}
                return Future.all(StreamSupport.stream(items.spliterator(), false)
                                               .map(i -> predicate.apply(context(), i)
                                                                  .map(match -> new Pair<>(i, match)))
                                               .toList()
                                 ).map(cf -> cf.<Pair<I, Boolean>>list().stream()
                                               .filter(Pair::value)
                                               .map(Pair::key)
                                               .toList());
            }));
        }

        default Batch<C, I, List<I>> filter(Predicate<I> predicate) {
            return Batch.of(context(), get().map(
                    i -> StreamSupport.stream(i.spliterator(), false).filter(predicate).toList()));
        }

        default Batch<C, I, List<I>> filterCtx(BiPredicate<C, I> predicate) {
            return Batch.of(context(), get().map(
                    i -> StreamSupport.stream(i.spliterator(), false)
                                      .filter(it -> predicate.test(context(), it)).toList()));
        }


        default <K> Pipeline<C, Map<K, List<I>>, ?> group(Function<I, K> classifier) {
            return new Simple<>(context(), get().map(items ->

                                                             StreamSupport.stream(items.spliterator(), false)
                                                                          .collect(
                                                                                  Collectors.groupingBy(
                                                                                          classifier))
                                                    ));
        }

        default <K, V> Pipeline<C, Map<K, List<V>>, ?> group(
                Function<I, K> classifier,
                Function<I, V> valueMapper) {
            return new Simple<>(context(), get().map(items -> StreamSupport
                                                             .stream(items.spliterator(), false)
                                                             .collect(
                                                                     Collectors.groupingBy(
                                                                             classifier,
                                                                             Collectors.mapping(
                                                                                     valueMapper,
                                                                                     Collectors.toList())
                                                                                          ))
                                                    ));
        }

        /// Group items by a key derived from the item and context
        default <K> Pipeline<C, Map<K, List<I>>, ?> groupCtx(BiFunction<C, I, K> classifier) {
            return new Simple<>(context(), get().map(items ->

                                                             StreamSupport.stream(items.spliterator(), false)
                                                                          .collect(Collectors.groupingBy(
                                                                                  item -> classifier.apply(
                                                                                          context(), item)))
                                                    ));
        }

        /// Group and transform values using context for both operations
        default <K, V> Pipeline<C, Map<K, List<V>>, ?> groupCtx(
                BiFunction<C, I, K> classifier,
                BiFunction<C, I, V> valueMapper) {
            return new Simple<>(context(), get().map(items -> StreamSupport
                                                             .stream(items.spliterator(), false)
                                                             .collect(Collectors.groupingBy(
                                                                     item -> classifier.apply(
                                                                             context(), item),
                                                                     Collectors.mapping(
                                                                             item -> valueMapper.apply(
                                                                                     context(),
                                                                                     item),
                                                                             Collectors.toList())
                                                                                           ))
                                                    ));
        }

        default <R> Pipeline<C, R, ?> reduce(R identity, BiFunction<R, I, R> accumulator) {
            return new Simple<>(context(), get().map(items -> StreamSupport
                                                             .stream(items.spliterator(), false)
                                                             .reduce(identity, accumulator,
                                                                     (a, b) -> a)
                                                    ));
        }

        default <R> Pipeline<C, R, ?> reduceCtx(R identity, TriFunction<C, R, I, R> accumulator) {
            return new Simple<>(context(), get().map(items -> StreamSupport
                                                             .stream(items.spliterator(), false)
                                                             .reduce(identity,
                                                                     (res, item) -> accumulator.apply(
                                                                             context(), res, item),
                                                                     (a, b) -> a)
                                                     // Combiner for sequential stream
                                                    ));
        }


        default <NC> Batch<NC, I, T> withContext(NC newContext) {
            return Batch.of(newContext, get());
        }

        default Pipeline<C, T, ?> pipeLine() {
            return Pipeline.of(context(), get());
        }
    }

    /// convert to a Batch pipe
    default <I, R extends Iterable<I>> Batch<C, I, R> batch(Function<T, R> map) {
        return Batch.of(context(), get().map(map));
    }

    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }


    /// retryable
    default <R> Pipeline<C, R, S> retry(Vertx vertx, RetryPolicy policy,
                                        Function<T, Future<R>> action) {
        return flatMap(val -> executeRetry(vertx, policy, () -> action.apply(val), 1, policy.initialDelayMs()));
    }

    default <R> Pipeline<C, R, S> retryCtx(Vertx vertx, RetryPolicy policy,
                                           BiFunction<C, T, Future<R>> action) {
        return flatMap(
                val -> executeRetry(vertx, policy, () -> action.apply(context(), val), 1, policy.initialDelayMs()));
    }

    private static <R> Future<R> executeRetry(
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
}
