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
import java.util.stream.Stream;
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

    //region Base
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

    /// factory from an existed future
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


    default Pipeline<C, T, S> raceWith(Pipeline<C, T, ?> other) {
        return wrap(Future.any(this.get(), other.get()).map(cf -> cf.resultAt(0)));
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
    //endregion

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

    //region Batch
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
                var list = StreamSupport.stream(items.spliterator(), false).toList();
                var checks = list.stream().map(predicate).toList();
                return Future.all(checks).map(cf -> {
                    var results = new ArrayList<I>();
                    for (int i = 0; i < list.size(); i++) {
                        if (cf.<Boolean>resultAt(i)) results.add(list.get(i));
                    }
                    return results;
                });
            }));
        }

        default Batch<C, I, List<I>> filterParCtx(BiFunction<C, I, Future<Boolean>> predicate) {
            return Batch.of(context(), get().flatMap(items -> {
                var list = StreamSupport.stream(items.spliterator(), false).toList();
                var checks = list.stream().map(i -> predicate.apply(context(), i)).toList();
                return Future.all(checks).map(cf -> {
                    var results = new ArrayList<I>();
                    for (int i = 0; i < list.size(); i++) {
                        if (cf.<Boolean>resultAt(i)) results.add(list.get(i));
                    }
                    return results;
                });
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
    //endregion

    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
    //region retry

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
    //endregion

    //region Monadic

    /// blueprint style process pipeline tools
    @FunctionalInterface
    interface Monadic<C, I, O> {

        Future<O> process(C context, I input);

        Monadic<?, ?, ?> IDENTITY = (ctx, in) -> Future.succeededFuture(in);


        @SuppressWarnings("unchecked")
        static <C, T> Monadic<C, T, T> identity() {
            return (Monadic<C, T, T>) IDENTITY;
        }

        static <C, I, O> Monadic<C, I, O> from(BiFunction<C, I, Future<O>> func) {
            return func::apply;
        }


        default <R> Monadic<C, I, R> map(Function<O, R> mapper) {
            return flatMap(o -> Future.succeededFuture(mapper.apply(o)));
        }

        default <R> Monadic<C, I, R> mapCtx(BiFunction<C, O, R> mapper) {
            return flatMapCtx((c, i) -> Future.succeededFuture(mapper.apply(c, i)));
        }

        default <R> Monadic<C, I, Optional<R>> mapOpt(Function<O, @Nullable R> mapper) {
            return map(mapper).map(Optional::ofNullable);
        }

        default <R> Monadic<C, I, Optional<R>> mapOptCtx(BiFunction<C, O, @Nullable R> mapper) {
            return mapCtx(mapper).map(Optional::ofNullable);
        }

        default <R> Monadic<C, I, R> flatMap(Function<O, Future<R>> mapper) {
            return flatMapCtx((ctx, o) -> mapper.apply(o));
        }

        @SuppressWarnings("unchecked")
        default <R> Monadic<C, I, R> flatMapCtx(BiFunction<C, O, Future<R>> mapper) {
            var steps = new LinkedList<Steps.Step<C, Object, Object>>();
            // Add current process as first step
            steps.add((c, ar) -> {
                if (ar.failed()) return Future.failedFuture(ar.cause());
                return (Future<Object>) this.process(c, (I) ar.result());
            });
            return new Steps<C, I, O>(steps).append(mapper);
        }

        default <R> Monadic<C, I, R> andThen(Monadic<C, O, R> next) {
            return flatMapCtx(next::process);
        }

        default Monadic<C, I, O> peek(Consumer<O> action) {
            return map(v -> {
                action.accept(v);
                return v;
            });
        }

        default Monadic<C, I, O> peekCtx(BiConsumer<C, O> action) {
            return mapCtx((ctx, v) -> {
                action.accept(ctx, v);
                return v;
            });
        }

        default Monadic<C, I, O> guard(Predicate<O> predicate, DomainError error) {
            return flatMap(v -> predicate.test(v) ? Future.succeededFuture(v) : Future.failedFuture(error));
        }

        default Monadic<C, I, O> guardCtx(BiPredicate<C, O> predicate, Function<C, DomainError> error) {
            return flatMapCtx((ctx, v) -> predicate.test(ctx, v)
                    ? Future.succeededFuture(v)
                    : Future.failedFuture(error.apply(ctx)));
        }

        default Monadic<C, I, O> recover(Function<Throwable, Future<O>> fallback) {
            return recoverCtx((ctx, err) -> fallback.apply(err));
        }

        default Monadic<C, I, O> recoverCtx(BiFunction<C, Throwable, Future<O>> fallback) {
            if (this instanceof Monadic.Steps<C, I, O> currentChain) {
                return currentChain.appendRecover(fallback);
            }
            return Monadic.<C, I>identity()
                          .andThen(this)
                          .recoverCtx(fallback);
        }

        default <E extends Throwable> Monadic<C, I, O> recover(Class<E> errorType, Function<E, Future<O>> fallback) {
            return recover(err -> {
                if (errorType.isInstance(err)) return fallback.apply(errorType.cast(err));
                return Future.failedFuture(err);
            });
        }

        default <E extends Throwable> Monadic<C, I, O> recoverCtx(Class<E> errorType,
                                                                  BiFunction<C, E, Future<O>> fallback) {
            return recoverCtx((c, e) -> {
                if (errorType.isInstance(e)) return fallback.apply(c, errorType.cast(e));
                return Future.failedFuture(e);
            });
        }

        default Monadic<C, I, O> recover(Predicate<Throwable> errorType, Function<Throwable, Future<O>> fallback) {
            return recoverCtx((ctx, err) -> {
                if (errorType.test(err)) {
                    return fallback.apply(err);
                }
                return Future.failedFuture(err);
            });
        }

        default Monadic<C, I, O> recoverCtx(BiPredicate<C, Throwable> errorType,
                                            BiFunction<C, Throwable, Future<O>> fallback) {
            return recoverCtx((ctx, err) -> {
                if (errorType.test(ctx, err)) {
                    return fallback.apply(ctx, err);
                }
                return Future.failedFuture(err);
            });
        }

        default Monadic<C, I, O> timeout(long amount, TimeUnit unit) {
            return (ctx, in) -> this.process(ctx, in).timeout(amount, unit);
        }

        default <R> Monadic<C, I, R> blocking(Vertx vertx, Function<O, R> handler) {
            return flatMap(o -> vertx.executeBlocking(() -> handler.apply(o)));
        }

        default <R> Monadic<C, I, R> blockingCtx(Vertx vertx, BiFunction<C, O, R> handler) {
            return flatMapCtx((ctx, out) -> vertx.executeBlocking(() -> handler.apply(ctx, out)));
        }

        default Monadic<C, I, O> retry(Vertx vertx, RetryPolicy policy) {
            return (ctx, in) -> executeRetry(vertx, policy, () -> this.process(ctx, in), 1, policy.initialDelayMs());
        }

        default <U, R> Monadic<C, I, R> zipPar(Monadic<C, I, U> other, BiFunction<O, U, R> combiner) {
            return (ctx, in) -> Future.all(this.process(ctx, in), other.process(ctx, in))
                                      .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1)));
        }

        default <U, R> Monadic<C, I, R> zipParCtx(Monadic<C, I, U> other, TriFunction<C, O, U, R> combiner) {
            return (ctx, in) -> Future.all(this.process(ctx, in), other.process(ctx, in))
                                      .map(cf -> combiner.apply(ctx, cf.resultAt(0), cf.resultAt(1)));
        }

        default <C2> Monadic<C2, I, O> withContext(Function<C2, C> contextBackMapper) {
            return (context2, input) -> {
                C context1 = contextBackMapper.apply(context2);
                return this.process(context1, input);
            };
        }

        default Monadic<C, I, O> sticky(Vertx vertx) {
            return flatMap(v -> {
                Promise<O> promise = Promise.promise();
                vertx.getOrCreateContext().runOnContext(x -> promise.complete(v));
                return promise.future();
            });
        }

        default <R> Monadic<C, I, R> fold(Function<Throwable, R> onFailure, Function<O, R> onSuccess) {
            return
                    map(onSuccess)
                            .recover(err -> Future.succeededFuture(onFailure.apply(err)));
        }

        default <R> Monadic<C, I, R> withBreaker(CircuitBreaker breaker, Function<O, Future<R>> action) {
            return flatMap(val -> breaker.execute(p -> action.apply(val).onComplete(p)));
        }

        @SuppressWarnings("unchecked")
        default Monadic<C, I, O> race(Function<I, Future<O>>... competitors) {
            return (ctx, in) -> Future.any(Stream
                                                   .concat(Arrays
                                                                   .stream(competitors)
                                                                   .map(c -> c.apply(in)),
                                                           Stream.of(this.process(ctx, in)))
                                                   .toList())
                                      .map(cf -> cf.resultAt(0));
        }

        @SuppressWarnings("unchecked")
        default Monadic<C, I, O> raceCtx(BiFunction<C, I, Future<O>>... competitors) {
            return (ctx, in) -> Future.any(Stream.concat(Arrays
                                                                 .stream(competitors)
                                                                 .map(c -> c.apply(ctx, in)),
                                                         Stream.of(this.process(ctx, in)))
                                                 .toList())
                                      .map(cf -> cf.resultAt(0));
        }


        default Monadic<C, I, O> raceWith(Monadic<C, I, O> other) {
            return (ctx, in) -> Future.any(this.process(ctx, in), other.process(ctx, in))
                                      .map(cf -> cf.resultAt(0));
        }

        default <E, X extends Iterable<E>> Batch<C, I, E, X> asBatch(Function<O, X> splitter) {
            return (ctx, in) -> this.process(ctx, in).map(splitter);
        }

        interface Batch<C, I, E, T extends Iterable<E>> {
            Future<T> process(C context, I input);

            default <C2> Batch<C2, I, E, T> withContext(Function<C2, C> contextBackMapper) {
                return (ctx2, in) -> this.process(contextBackMapper.apply(ctx2), in);
            }

            static <C, I, E, T extends Iterable<E>> Batch<C, I, E, T> from(Monadic<C, I, T> pipe) {
                return pipe::process;
            }

            default <R> Batch<C, I, R, List<R>> mapEach(Function<E, R> mapper) {
                return (ctx, in) -> this.process(ctx, in).map(items ->
                                                                      StreamSupport.stream(items.spliterator(), false)
                                                                                   .map(mapper).toList());
            }

            default <R> Batch<C, I, R, List<R>> mapEachCtx(BiFunction<C, E, R> mapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .map(items -> StreamSupport
                                                .stream(items.spliterator(), false)
                                                .map(i -> mapper.apply(ctx, i)).toList());
            }

            default <R> Batch<C, I, R, List<R>> mapEachPar(Function<E, Future<R>> mapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .flatMap(items -> Future
                                                         .all(StreamSupport
                                                                      .stream(items.spliterator(), false)
                                                                      .map(mapper)
                                                                      .toList())
                                                         .map(cf -> cf.list())
                                                );
            }

            default <R> Batch<C, I, R, List<R>> mapEachParCtx(BiFunction<C, E, Future<R>> mapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .flatMap(items -> Future
                                                         .all(StreamSupport
                                                                      .stream(items.spliterator(), false)
                                                                      .map(i -> mapper.apply(ctx, i))
                                                                      .toList())
                                                         .map(CompositeFuture::list)
                                                );
            }

            default <R> Batch<C, I, R, List<R>> mapEachPar(int concurrency, Function<E, Future<R>> mapper) {
                return (ctx, in) -> this.process(ctx, in).flatMap(items ->
                                                                          mapParallel(items, concurrency, mapper));
            }

            default <R> Batch<C, I, R, List<R>> mapEachParCtx(int concurrency, BiFunction<C, E, Future<R>> mapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .flatMap(items -> mapParallel(items, concurrency, i -> mapper.apply(ctx, i)));
            }

            default Batch<C, I, E, List<E>> filter(Predicate<E> predicate) {
                return (ctx, in) -> this.process(ctx, in).map(items ->
                                                                      StreamSupport.stream(items.spliterator(), false)
                                                                                   .filter(predicate).toList());
            }

            default Batch<C, I, E, List<E>> filterCtx(BiPredicate<C, E> predicate) {
                return (ctx, in) -> this.process(ctx, in)
                                        .map(items -> StreamSupport
                                                .stream(items.spliterator(), false)
                                                .filter(item -> predicate.test(ctx, item))
                                                .toList()
                                            );
            }

            default Batch<C, I, E, List<E>> filterPar(Function<E, Future<Boolean>> predicate) {
                return (ctx, in) -> this.process(ctx, in).flatMap(items -> {
                    var list = StreamSupport.stream(items.spliterator(), false).toList();
                    var checks = list.stream().map(predicate).toList();
                    return listFuture((List<E>) list, checks);
                });
            }

            default Batch<C, I, E, List<E>> filterParCtx(BiFunction<C, E, Future<Boolean>> predicate) {
                return (ctx, in) -> this.process(ctx, in).flatMap(items -> {
                    var list = StreamSupport.stream(items.spliterator(), false).toList();
                    var checks = list.stream().map(i -> predicate.apply(ctx, i)).toList();
                    return listFuture((List<E>) list, checks);
                });
            }

            private static <E> Future<List<E>> listFuture(List<E> list, List<Future<Boolean>> checks) {
                return Future.all(checks).map(cf -> {
                    var results = new ArrayList<E>();
                    for (int i = 0; i < list.size(); i++) {
                        if (cf.<Boolean>resultAt(i)) results.add(list.get(i));
                    }
                    return results;
                });
            }

            default <K> Monadic<C, I, Map<K, List<E>>> group(Function<E, K> classifier) {
                return (ctx, in) -> this.process(ctx, in).map(items ->
                                                                      StreamSupport.stream(items.spliterator(), false)
                                                                                   .collect(Collectors.groupingBy(
                                                                                           classifier)));
            }

            default <K> Monadic<C, I, Map<K, List<E>>> groupCtx(BiFunction<C, E, K> classifier) {
                return (ctx, in) -> this.process(ctx, in)
                                        .map(items -> StreamSupport
                                                .stream(items.spliterator(), false)
                                                .collect(Collectors.groupingBy(
                                                        i -> classifier.apply(ctx, i))));
            }

            default <K, V> Monadic<C, I, Map<K, List<V>>> group(Function<E, K> classifier, Function<E, V> valueMapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .map(items -> StreamSupport
                                                .stream(items.spliterator(), false)
                                                .collect(Collectors.groupingBy(classifier,
                                                                               Collectors.mapping(valueMapper,
                                                                                                  Collectors.toList())))
                                            );
            }

            default <K, V> Monadic<C, I, Map<K, List<V>>> groupCtx(BiFunction<C, E, K> classifier,
                                                                   BiFunction<C, E, V> valueMapper) {
                return (ctx, in) -> this.process(ctx, in)
                                        .map(items -> StreamSupport
                                                .stream(items.spliterator(), false)
                                                .collect(Collectors
                                                                 .groupingBy(i -> classifier.apply(ctx, i),
                                                                             Collectors.mapping(
                                                                                     i -> valueMapper.apply(ctx, i),
                                                                                     Collectors.toList())))
                                            );
            }


            default <R> Monadic<C, I, R> reduce(R identity, BiFunction<R, E, R> accumulator) {
                return (ctx, in) -> this.process(ctx, in).map(items ->
                                                                      StreamSupport.stream(items.spliterator(), false)
                                                                                   .reduce(identity, accumulator,
                                                                                           (a, b) -> a));
            }

            default <R> Monadic<C, I, R> reduceCtx(R identity, TriFunction<C, R, E, R> accumulator) {
                return (ctx, in) -> this.process(ctx, in).map(items ->
                                                                      StreamSupport.stream(items.spliterator(), false)
                                                                                   .reduce(identity,
                                                                                           (res, item) -> accumulator.apply(
                                                                                                   ctx, res, item),
                                                                                           (a, b) -> a));
            }


            private static <E, R> Future<List<R>> mapParallel(Iterable<E> items, int concurrency,
                                                              Function<E, Future<R>> mapper) {
                List<E> list = StreamSupport.stream(items.spliterator(), false).toList();
                if (list.isEmpty()) return Future.succeededFuture(List.of());

                Promise<List<R>> promise = Promise.promise();
                AtomicInteger nextIndex = new AtomicInteger(0);
                AtomicInteger remaining = new AtomicInteger(list.size());
                @SuppressWarnings("unchecked")
                R[] results = (R[]) new Object[list.size()];
                var worker = new Runnable() {
                    @Override
                    public void run() {
                        // Loop as long as we have items and the promise isn't done
                        while (!promise.future().isComplete()) {
                            int i = nextIndex.getAndIncrement();
                            if (i >= list.size()) return; // No more work
                            Future<R> fut;
                            try {
                                fut = mapper.apply(list.get(i));
                            } catch (Throwable t) {
                                promise.tryFail(t);
                                return;
                            }
                            if (fut.isComplete()) {
                                // FAST PATH: Handle synchronous completion without recursion
                                if (fut.succeeded()) {
                                    results[i] = fut.result();
                                    if (remaining.decrementAndGet() == 0) {
                                        promise.tryComplete(Arrays.asList(results));
                                        return;
                                    }
                                    // Continue loop to pick up next item immediately
                                } else {
                                    promise.tryFail(fut.cause());
                                    return;
                                }
                            } else {
                                // ASYNC PATH: Register callback
                                fut.onComplete(ar -> {
                                    if (ar.succeeded()) {
                                        results[i] = ar.result();
                                        if (remaining.decrementAndGet() == 0) {
                                            promise.tryComplete(Arrays.asList(results));
                                        } else {
                                            // Resume work on this thread
                                            this.run();
                                        }
                                    } else {
                                        promise.tryFail(ar.cause());
                                    }
                                });
                                // Break the loop, the callback will resume execution
                                return;
                            }
                        }
                    }
                };
                int actualConcurrency = Math.min(concurrency, list.size());
                for (int i = 0; i < actualConcurrency; i++) worker.run();
                return promise.future();
            }
        }

        record Steps<C, I, O>(List<Step<C, Object, Object>> steps) implements Monadic<C, I, O> {
            interface Step<C, I, O> extends BiFunction<C, AsyncResult<I>, Future<O>> {}

            @Override
            @SuppressWarnings("unchecked")
            public Future<O> process(C context, I input) {
                Future<Object> current = Future.succeededFuture(input);
                for (var step : steps) {
                    current = current.transform(val -> step.apply(context, val));
                }
                return (Future<O>) current;
            }

            @SuppressWarnings("unchecked")
            public <NextO> Steps<C, I, NextO> append(BiFunction<C, O, Future<NextO>> next) {
                var newSteps = new LinkedList<>(steps);
                newSteps.add((ctx, in) -> in.succeeded() ? (Future<Object>) next.apply(ctx,
                                                                                       (O) in.result()) : Future.failedFuture(
                        in.cause()));
                return new Steps<>(newSteps);
            }

            @SuppressWarnings("unchecked")
            public <NextO> Steps<C, I, NextO> appendRecover(BiFunction<C, Throwable, Future<NextO>> recovery) {
                var newSteps = new LinkedList<>(steps);
                newSteps.add((ctx, ar) -> ar.failed() ? (Future<Object>) recovery.apply(ctx,
                                                                                        ar.cause()) : Future.succeededFuture(
                        ar.result()));
                return new Steps<>(newSteps);
            }
        }
    }
    //endregion
}
