package vat.api.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import vat.api.DomainError;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/// blueprint style process pipeline tools
@SuppressWarnings("unused")
@FunctionalInterface
public
interface Monadic<C, I, O> {
    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    Future<O> process(C context, I input);

    Monadic<?, ?, ?> IDENTITY = (ctx, in) -> Future.succeededFuture(in);


    @SuppressWarnings("unchecked")
    static <C, T> Monadic<C, T, T> identity() {
        return (Monadic<C, T, T>) IDENTITY;
    }

    static <C, I, O> Monadic<C, I, O> from(BiFunction<C, I, Future<O>> func) {
        return func::apply;
    }
    //region basic

    @SuppressWarnings("unchecked")
    default <R> Monadic<C, I, R> flatMapCtx(BiFunction<C, O, Future<R>> mapper) {
        return switch (this) {
            case Finalized<C, I, O>(Steps.Step<C, Object, Object>[] fSteps) -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>(List.of(fSteps));
                yield new Steps<C, I, O>(steps).append(mapper);
            }
            case Steps<C, I, O> currentChain -> currentChain.append(mapper);
            default -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>();
                steps.add((c, ar) -> ar.failed()
                        ? Future.failedFuture(ar.cause())
                        : (Future<Object>) this.process(c, (I) ar.result()));
                yield new Steps<C, I, O>(steps).append(mapper);
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <R> Monadic<C, I, R> andThen(Monadic<C, O, R> next) {
        return switch (this) {
            case Finalized<C, I, O>(Steps.Step<C, Object, Object>[] fSteps) -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>(List.of(fSteps));
                yield new Steps<C, I, O>(steps).appendPipeline(next);
            }
            case Steps<C, I, O> current -> current.appendPipeline(next);
            default -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>();
                steps.add((c, ar) -> {
                    if (ar.failed()) return Future.failedFuture(ar.cause());
                    return (Future<Object>) this.process(c, (I) ar.result());
                });
                yield new Steps<C, I, O>(steps).appendPipeline(next);
            }
        };

    }

    @SuppressWarnings("unchecked")
    default Monadic<C, I, O> recoverCtx(BiFunction<C, Throwable, Future<O>> fallback) {
        return switch (this) {
            case Finalized<C, I, O>(Steps.Step<C, Object, Object>[] fSteps) -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>(List.of(fSteps));
                steps.add((ctx, ar) -> ar.failed()
                        ? (Future<Object>) fallback.apply(ctx, ar.cause())
                        : Future.succeededFuture(ar.result()));
                yield new Steps<>(steps);
            }
            case Steps<C, I, O> currentChain -> currentChain.appendRecover(fallback);
            default -> {
                List<Steps.Step<C, Object, Object>> steps = new ArrayList<>();
                steps.add((c, ar) -> {
                    if (ar.failed()) return Future.failedFuture(ar.cause());
                    return (Future<Object>) this.process(c, (I) ar.result());
                });
                yield new Steps<C, I, O>(steps).appendRecover(fallback);
            }
        };
    }
    //endregion
    //region core

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


    default Monadic<C, I, O> peek(Predicate<O> cond, Consumer<O> action) {
        return map(v -> {
            if (cond.test(v))
                action.accept(v);
            return v;
        });
    }

    default Monadic<C, I, O> peekCtx(BiPredicate<C, O> cond, BiConsumer<C, O> action) {
        return mapCtx((ctx, v) -> {
            if (cond.test(ctx, v))
                action.accept(ctx, v);
            return v;
        });
    }

    default <R> Monadic<C, I, R> value(R value) {
        return mapCtx((ctx, v) -> value);
    }

    default <R> Monadic<C, I, R> valueCtx(Function<C, R> value) {
        return mapCtx((ctx, v) -> value.apply(ctx));
    }

    default <U, R> Monadic<C, I, R> zipPar(Monadic<C, I, U> other, BiFunction<O, U, R> combiner) {
        return (ctx, in) -> Future.all(this.process(ctx, in), other.process(ctx, in))
                .map(cf -> combiner.apply(cf.resultAt(0), cf.resultAt(1)));
    }

    default <U, R> Monadic<C, I, R> zipParCtx(Monadic<C, I, U> other, TriFunction<C, O, U, R> combiner) {
        return (ctx, in) -> Future.all(this.process(ctx, in), other.process(ctx, in))
                .map(cf -> combiner.apply(ctx, cf.resultAt(0), cf.resultAt(1)));
    }
    //endregion

    //region guarding

    /// check value
    default Monadic<C, I, O> should(Expectation<? super O> action) {
        return (c, i) -> this.process(c, i).expecting(action);
    }

    default Monadic<C, I, O> guard(Predicate<O> predicate, DomainError error) {
        return flatMap(v -> predicate.test(v) ? Future.succeededFuture(v) : Future.failedFuture(error));
    }

    default Monadic<C, I, O> guardCtx(BiPredicate<C, O> predicate, Function<C, DomainError> error) {
        return flatMapCtx((ctx, v) -> predicate.test(ctx, v)
                ? Future.succeededFuture(v)
                : Future.failedFuture(error.apply(ctx)));
    }
    //endregion

    //region failure
    default Monadic<C, I, O> recover(Function<Throwable, Future<O>> fallback) {
        return recoverCtx((ctx, err) -> fallback.apply(err));
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

    default Monadic<C, I, O> onError(Handler<Throwable> handler) {
        return (c, i) -> this.process(c, i).onFailure(handler);
    }

    default Monadic<C, I, O> onErrorCtx(BiConsumer<C, Throwable> handler) {
        return (c, i) -> this.process(c, i).onFailure(e -> handler.accept(c, e));
    }

    default <R> Monadic<C, I, R> fold(Function<O, R> onSuccess, Function<Throwable, R> onFailure) {
        return
                map(onSuccess)
                        .recover(err -> Future.succeededFuture(onFailure.apply(err)));
    }

    default <R> Monadic<C, I, R> foldCtx(BiFunction<C, O, R> onSuccess, BiFunction<C, Throwable, R> onFailure) {
        return
                mapCtx(onSuccess)
                        .recoverCtx((c, err) -> Future.succeededFuture(onFailure.apply(c, err)));
    }

    //endregion

    //region resilience
    default Monadic<C, I, O> retry(Vertx vertx, RetryPolicy policy) {
        return (ctx, in) -> executeRetry(vertx, policy, () -> this.process(ctx, in), 1, policy.initialDelayMs());
    }

    default <R> Monadic<C, I, R> withBreaker(CircuitBreaker breaker, Function<O, Future<R>> action) {
        return flatMap(val -> breaker.execute(p -> action.apply(val).onComplete(p)));
    }

    default <R> Monadic<C, I, R> withBreakerCtx(Function<C, CircuitBreaker> breaker, BiFunction<C, O, Future<R>> action) {
        return flatMapCtx((c, val) -> breaker.apply(c).execute(p -> action.apply(c, val).onComplete(p)));
    }

    @SuppressWarnings("unchecked")

    default <R> Monadic<C, I, R> race(Function<O, Future<R>>... competitors) {
        return flatMapCtx((ctx, in) -> Future
                .any(Arrays.stream(competitors)
                        .map(c -> c.apply(in))
                        .toList())
                .map(Monadic::firstResult));
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private static <T> T firstResult(CompositeFuture cf) {
        return IntStream.range(0, cf.size()).filter(cf::succeeded).mapToObj(cf::resultAt).map(x -> (T) x).findFirst().orElseThrow(() -> cf.cause(0));
    }

    @SuppressWarnings("unchecked")
    default <R> Monadic<C, I, R> raceCtx(BiFunction<C, O, Future<R>>... competitors) {
        return flatMapCtx((ctx, in) -> Future
                .any(Arrays.stream(competitors)
                        .map(c -> c.apply(ctx, in))

                        .toList())
                .map(Monadic::firstResult));
    }


    default Monadic<C, I, O> raceWith(Monadic<C, I, O> other) {
        return (ctx, in) -> Future.any(this.process(ctx, in), other.process(ctx, in))
                .map(Monadic::firstResult);
    }

    default Monadic<C, I, O> delay(Vertx vertx, long delay, TimeUnit unit) {
        return (ctx, in) -> this.process(ctx, in)
                .flatMap(val -> {
                    Promise<O> p = Promise.promise();
                    vertx.setTimer(unit.toMillis(delay), id -> p.complete(val));
                    return p.future();
                });
    }
    //endregion

    default Monadic<C, I, O> timeout(long amount, TimeUnit unit) {
        return (ctx, in) -> this.process(ctx, in).timeout(amount, unit);
    }

    default <R> Monadic<C, I, R> blocking(Vertx vertx, Function<O, R> handler) {
        return flatMap(o -> vertx.executeBlocking(() -> handler.apply(o)));
    }

    default <R> Monadic<C, I, R> blockingCtx(Vertx vertx, BiFunction<C, O, R> handler) {
        return flatMapCtx((ctx, out) -> vertx.executeBlocking(() -> handler.apply(ctx, out)));
    }

    default Monadic<C, I, O> eventually(Supplier<Future<Void>> action) {
        return (c, i) -> this.process(c, i).eventually(action);
    }

    default Monadic<C, I, O> eventuallyCtx(Function<C, Future<Void>> action) {
        return (c, i) -> this.process(c, i).eventually(() -> action.apply(c));
    }

    default Monadic<C, I, O> sticky(Vertx vertx) {
        return flatMap(v -> {
            Promise<O> promise = Promise.promise();
            vertx.getOrCreateContext().runOnContext(x -> promise.complete(v));
            return promise.future();
        });
    }

    default <C2> Monadic<C2, I, O> withContext(Function<C2, C> contextBackMapper) {
        return (context2, input) -> {
            C context1 = contextBackMapper.apply(context2);
            return this.process(context1, input);
        };
    }

    //region resource closure
    default <R> Monadic<C, I, R> bracket(Function<O, Future<R>> use,
                                         Function<O, Future<Void>> release) {
        return flatMap(resource -> use
                .apply(resource)
                .eventually(() -> release.apply(resource)));
    }

    default <R> Monadic<C, I, R> bracketCtx(BiFunction<C, O, Future<R>> use,
                                            BiFunction<C, O, Future<Void>> release) {
        return flatMapCtx((c, resource) -> use
                .apply(c, resource)
                .eventually(() -> release.apply(c, resource)));
    }
    //endregion

    //region condition operation
    default Monadic<C, I, O> check(Predicate<O> predicate, Function<O, Future<Void>> validation) {
        return flatMap(v -> {
            if (predicate.test(v)) {
                return validation.apply(v).map(ignore -> v);
            }
            return Future.succeededFuture(v);
        });
    }

    default Monadic<C, I, O> checkCtx(BiPredicate<C, O> predicate, BiFunction<C, O, Future<Void>> validation) {
        return flatMapCtx((c, v) -> {
            if (predicate.test(c, v)) {
                return validation.apply(c, v).map(ignore -> v);
            }
            return Future.succeededFuture(v);
        });
    }
    //endregion

    //region branch
    default <R> Monadic<C, I, R> match(
            Predicate<O> predicate,
            Function<O, Future<R>> onTrue,
            Function<O, Future<R>> onFalse) {
        return flatMap(val -> predicate.test(val)
                ? onTrue.apply(val)
                : onFalse.apply(val)
        );
    }

    default <R> Monadic<C, I, R> matchCtx(
            BiPredicate<C, O> predicate,
            BiFunction<C, O, Future<R>> onTrue,
            BiFunction<C, O, Future<R>> onFalse) {
        return flatMapCtx((ctx, val) -> predicate.test(ctx, val)
                ? onTrue.apply(ctx, val)
                : onFalse.apply(ctx, val)
        );
    }

    /// conditional applicative
    default Monadic<C, I, O> flatMapIf(Predicate<O> predicate, Function<O, Future<O>> action) {
        return flatMap(val -> predicate.test(val)
                ? action.apply(val)
                : Future.succeededFuture(val)
        );
    }

    default Monadic<C, I, O> flatMapIfCtx(BiPredicate<C, O> predicate, BiFunction<C, O, Future<O>> action) {
        return flatMapCtx((c, val) -> predicate.test(c, val)
                ? action.apply(c, val)
                : Future.succeededFuture(val)
        );
    }
    //endregion

    //region batch
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

        default <R> Monadic<C, I, R> collect(Function<T, R> collector) {
            return (ctx, in) -> this.process(ctx, in).map(collector);
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

    //endregion
    record Finalized<C, I, O>(Steps.Step<C, Object, Object>[] steps) implements Monadic<C, I, O> {
        @Override
        @SuppressWarnings("unchecked")
        public Future<O> process(C context, I input) {
            Promise<Object> finalPromise = Promise.promise();
            processStep(context, 0, Future.succeededFuture(input), finalPromise);
            return (Future<O>) finalPromise.future();
        }

        private void processStep(C ctx, int i, AsyncResult<Object> ar, Promise<Object> p) {
            if (i >= steps.length) {
                p.handle(ar);
                return;
            }
            AsyncResult<Object> last = ar;
            while (i < steps.length) {
                Future<Object> f;
                try {
                    f = steps[i].apply(ctx, last);
                } catch (Throwable t) {
                    f = Future.failedFuture(t);
                }
                //noinspection ConstantValue
                if (f == null) f = Future.failedFuture(new NullPointerException("returns null future at " + i));

                if (!f.isComplete()) {
                    final int next = i + 1;
                    f.onComplete(res -> processStep(ctx, next, res, p));
                    return;
                }
                last = f;
                i++;
            }
            p.handle(last);
        }
    }

    /// create an optimized monadic
    @SuppressWarnings("unchecked")
    default Monadic<C, I, O> finalization() {
        if (this instanceof Monadic.Finalized<C, I, O> f) return f;
        //noinspection DeconstructionCanBeUsed
        if (this instanceof Monadic.Steps<?, ?, ?> s) return new Finalized<>(s.steps.toArray(Steps.Step[]::new));
        List<Steps.Step<C, Object, Object>> steps = new ArrayList<>();
        steps.add((c, ar) -> {
            if (ar.failed()) return Future.failedFuture(ar.cause());
            return (Future<Object>) this.process(c, (I) ar.result());
        });
        return new Finalized<>(steps.toArray(Steps.Step[]::new));
    }

    record Steps<C, I, O>(List<Steps.Step<C, Object, Object>> steps) implements Monadic<C, I, O> {
        interface Step<C, I, O> extends BiFunction<C, AsyncResult<I>, Future<O>> {
        }

        @Override
        @SuppressWarnings("unchecked")
        public Future<O> process(C context, I input) {
         /*   Future<Object> current = Future.succeededFuture(input);
            for (var step : steps) {
                current = current.transform(val -> step.apply(context, val));
            }
            return (Future<O>) current;*/
            Promise<Object> finalPromise = Promise.promise();
            processStep(context, 0, Future.succeededFuture(input), finalPromise);
            return (Future<O>) finalPromise.future();
        }

        private void processStep(C ctx, int i, AsyncResult<Object> ar, Promise<Object> p) {
            if (i >= steps.size()) {
                p.handle(ar);
                return;
            }
            AsyncResult<Object> last = ar;
            while (i < steps.size()) {
                Future<Object> f;
                try {
                    f = steps.get(i).apply(ctx, last);
                } catch (Throwable t) {
                    f = Future.failedFuture(t);
                }
                //noinspection ConstantValue
                if (f == null) f = Future.failedFuture(new NullPointerException("returns null future at " + i));

                if (!f.isComplete()) {
                    final int next = i + 1;
                    f.onComplete(res -> processStep(ctx, next, res, p));
                    return;
                }
                last = f;
                i++;
            }
            p.handle(last);
        }

        @SuppressWarnings("unchecked")
        public <NextO> Steps<C, I, NextO> append(BiFunction<C, O, Future<NextO>> next) {
            var newSteps = new ArrayList<>(steps);
            newSteps.add((ctx, in) -> in.succeeded()
                    ? (Future<Object>) next.apply(ctx, (O) in.result())
                    : Future.failedFuture(in.cause()));
            return new Steps<>(newSteps);
        }

        @SuppressWarnings("unchecked")
        public <NextO> Steps<C, I, NextO> appendRecover(BiFunction<C, Throwable, Future<NextO>> recovery) {
            var newSteps = new ArrayList<>(steps);
            newSteps.add((ctx, ar) -> ar.failed() ? (Future<Object>) recovery.apply(ctx,
                    ar.cause()) : Future.succeededFuture(
                    ar.result()));
            return new Steps<>(newSteps);
        }

        @SuppressWarnings("unchecked")
        public <NextO> Steps<C, I, NextO> appendPipeline(Monadic<C, O, NextO> next) {
            switch (next) {
                case Finalized<C, O, NextO> steps1 -> {
                    var nextSteps = steps1.steps;
                    var combined = new ArrayList<Step<C, Object, Object>>(steps.size() + nextSteps.length);
                    combined.addAll(steps);
                    combined.addAll(List.of(nextSteps));
                    return new Steps<>(combined);
                }
                case Steps<C, O, NextO> steps1 -> {
                    var nextSteps = steps1.steps;
                    var combined = new ArrayList<Step<C, Object, Object>>(steps.size() + nextSteps.size());
                    combined.addAll(steps);
                    combined.addAll(nextSteps);
                    return new Steps<>(combined);
                }
                default -> {
                    var newSteps = new ArrayList<>(steps);
                    newSteps.add((ctx, in) -> in.succeeded()
                            ? (Future<Object>) next.process(ctx, (O) in.result())
                            : Future.failedFuture(in.cause()));
                    return new Steps<>(newSteps);
                }
            }

        }

        @Override
        public String diagram() {
            StringBuilder sb = new StringBuilder("graph TD\n");
            sb.append("  Start((Start)) --> Step_0\n");
            for (int i = 0; i < steps.size(); i++) {
                String name = steps.get(i).getClass().getSimpleName();
                if (name.contains("$$Lambda")) name = "Lambda_Step_" + i;

                sb.append("  Step_").append(i).append("[").append(name).append("]");
                if (i < steps.size() - 1) {
                    sb.append(" --> Step_").append(i + 1).append("\n");
                } else {
                    sb.append(" --> End((End))\n");
                }
            }
            return sb.toString();
        }
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
                    t ->
                            t instanceof java.io.IOException
                            || t.getMessage().contains("timeout"));
        }
    }


    default String diagram() {
        return "graph TD\n  Start((Start)) --> " + this.getClass().getSimpleName() + "((End))";
    }


}
