package vat.api.utils;

import io.vertx.core.*;
import lombok.SneakyThrows;
import org.jooq.lambda.Seq;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.tuple.*;
import vat.api.DomainError;
import vat.api.meta.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

///  generic functions
///
/// @author Zen.Liu
/// @since 2025-10-26

public interface Fn {
    static <T> T fail(RuntimeException err) {
        throw err;
    }

    static <T> T noneNull(T v, Supplier<DomainError> err) {
        if (v == null) throw err.get();
        return v;
    }

    static <S, T> T parseNullable(S value, Function<S, T> o) {
        return value == null ? null : o.apply(value);
    }

    //region function
    static <T, R> Stream<R> cast(Stream<T> t, Class<R> type) {
        return t.filter(type::isInstance).map(type::cast);
    }

    @SneakyThrows
    static <T> T asserts(T v, Predicate<T> check, Function<T, Exception> ex) {
        if (check.test(v)) return v;
        throw ex == null ? DomainError.System.badRequest("value invalid") : ex.apply(v);
    }

    @SneakyThrows
    static <T> T asserts(T v, Predicate<T> check) {
        return asserts(v, check, null);
    }

    static Boolean isTrue(Boolean value) {
        if (value == null || !value) throw DomainError.System.notAcceptable("not acceptable");
        return true;
    }

    static <T> T apply(T t, Consumer<T> o) {
        if (t == null) return null;
        o.accept(t);
        return t;
    }

    static boolean notBlank(@Nullable String v) {
        return v != null && !v.isBlank();
    }

    static String nonBlank(String s) {
        if (!notBlank(s)) throw new IllegalStateException("value require not blank");
        return s;
    }


    static <T> T log(T v, boolean cond, BiConsumer<String, Object[]> log, String pattern, Object... argument) {
        if (cond) log.accept(pattern, argument.length == 0 ? new Object[]{v} : Many.prepend(argument, v));
        return v;
    }

    static <T, R> T log(T v, boolean cond, Function<T, R> map, BiConsumer<String, Object[]> log, String pattern, Object... argument) {
        if (cond)
            log.accept(pattern, argument.length == 0 ? new Object[]{map.apply(v)} : Many.prepend(argument, map.apply(v)));
        return v;
    }
    //endregion

    //region operator
    static <T, R> Function<Stream<T>, Stream<R>> cast(Class<R> type) {
        return t -> t.filter(type::isInstance).map(type::cast);
    }

    static UnaryOperator<Boolean> isTrue(Supplier<DomainError> err) {
        return v -> {
            if (v) throw err.get();
            return false;
        };
    }

    /// convert consumer to Function
    static <T> Function<T, T> peek(Consumer<T> o) {
        return t -> {
            o.accept(t);
            return t;
        };
    }

    /// throw domain error if not match predicate
    static <T> Function<T, T> expect(Supplier<DomainError> err, Predicate<T> o) {
        return t -> {
            if (!o.test(t)) throw err.get();
            return t;
        };
    }

    /// convert to null if not match predicate
    static <T> Function<T, T> empty(Predicate<T> o) {
        return t -> {
            if (!o.test(t)) return null;
            return t;
        };
    }

    static <T> Function<T, Optional<T>> optional() {
        return Optional::ofNullable;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    Function<Optional, Object> OR_ELSE_NULL = o -> o.orElse(null);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> Function<Optional<T>, T> orNull() {
        return (Function<Optional<T>, T>) (Function) OR_ELSE_NULL;
    }

    static <T> Function<T, T> predicate(Predicate<T> cond, Function<T, T> onTrue, Function<T, T> onFalse) {
        return t -> cond.test(t) ? onTrue.apply(t) : onFalse.apply(t);
    }

    static <T> Function<T, T> condition(BooleanSupplier cond, Function<T, T> onTrue, Function<T, T> onFalse) {
        return t -> cond.getAsBoolean() ? onTrue.apply(t) : onFalse.apply(t);
    }

    /// convert BiFunction to Function with supplied first value
    static <R, T> Function<T, T> curryFst(Supplier<R> v, BiFunction<R, T, T> o) {
        return t -> o.apply(v.get(), t);
    }

    static <R, T> Function<T, T> closureFst(R v, BiFunction<R, T, T> o) {
        return t -> o.apply(v, t);
    }

    /// convert BiFunction to Function with supplied secondary value
    static <R, T> Function<T, T> currySnd(Supplier<R> v, BiFunction<T, R, T> o) {
        return t -> o.apply(t, v.get());
    }

    static <R, T> Function<T, T> closureSnd(R v, BiFunction<T, R, T> o) {
        return t -> o.apply(t, v);
    }

    /// convert BiFunction to Function with supplied first value supplier
    static <R, T> Function<T, T> provideFst(Supplier<R> v, BiFunction<Supplier<R>, T, T> o) {
        return t -> o.apply(v, t);
    }

    /// convert BiFunction to Function with supplied secondary value supplier
    static <R, T> Function<T, T> provideSnd(Supplier<R> v, BiFunction<T, Supplier<R>, T> o) {
        return t -> o.apply(t, v);
    }


    /// compose chain function to UnaryOperator
    static <R, T> Function<T, T> compose(Function<T, R> o, Function<R, T> r) {
        return r.compose(o);
    }

    /// pipe a function , Same as Function.andThen
    static <T, M, R> Function<T, R> pipe(Function<T, M> o, Function<M, R> r) {
        return o.andThen(r);
    }

    static <I, O> Supplier<O> supplierClosure(I v, Function<I, O> fn) {
        return () -> fn.apply(v);
    }

    static <I, O> Supplier<O> supplier(Supplier<I> v, Function<I, O> fn) {
        return () -> fn.apply(v.get());
    }

    static <I, O, I0, O0> Function<I, O> pack(Function<I, I0> get, Function<I, O0> act, Function2<I0, O0, O> set) {
        return u -> {
            var p = get.apply(u);
            var o = act.apply(u);
            return set.apply(p, o);
        };
    }

    /// Mapping input I to combined value O
    static <I, O, V> Function<I, O> pipe(Supplier<V> act, Function2<I, V, O> set) {
        return u -> set.apply(u, act.get());
    }

    /// Mapping input I with element I0 to value O0 and combine to O
    static <I, O, I0, O0> Function<I, O> pipe(Function<I, I0> get, Function<I0, O0> act, Function2<I, O0, O> set) {
        return u -> set.apply(u, act.apply(get.apply(u)));
    }


    static <I, V0, V1> Function<I, V1> map(Function<I, V0> mapper, Function<V0, V1> cast) {
        return t -> cast.apply(mapper.apply(t));
    }

    static <I, V0, V1> BiFunction<I, V1, I> biMap(BiFunction<I, V0, I> mapper, Function<V1, V0> cast) {
        return (i, t) -> mapper.apply(i, cast.apply(t));
    }

    static <T, R> Function<T, R> nonNull(Function<T, R> mapper) {
        return t -> {
            if (t == null) throw new IllegalArgumentException("argument required none null");
            var v = mapper.apply(t);
            if (v == null) throw DomainError.System.badRequest("corrupted data, should exists");
            return v;
        };
    }

    static <T0, T, R> BiFunction<T0, T, R> nonNull(BiFunction<T0, T, R> mapper) {
        return (t0, t) -> {
            if (t0 == null || t == null) throw new IllegalArgumentException("argument required none null");
            var v = mapper.apply(t0, t);
            if (v == null) throw DomainError.System.badRequest("corrupted data, should exists");
            return v;
        };
    }

    static <T, R> Function<T, R> nullable(Function<T, R> mapper) {
        return t -> t == null ? null : mapper.apply(t);
    }

    static <T0, T, R> BiFunction<T0, T, R> nullable(BiFunction<T0, T, R> mapper) {
        return (t0, t) -> t0 == null || t == null ? null : mapper.apply(t0, t);
    }

    static <T> Function<T, T> equal(T val) {
        return t -> {
            if (!Objects.equals(t, val)) throw DomainError.System.notAcceptable("not acceptable");
            return t;
        };
    }

    static <T> Function<T, T> lengthEqual(int val, ToIntFunction<T> len) {
        return t -> {
            if (len.applyAsInt(t) != val) throw DomainError.System.notAcceptable("not acceptable");
            return t;
        };
    }

    static <T> Function<T, T> lengthEqual(int val, ToIntFunction<T> len, IntFunction<DomainError> error) {
        return t -> {
            if (len.applyAsInt(t) != val)
                throw error == null ? DomainError.System.notAcceptable("not acceptable") : error.apply(len.applyAsInt(t));
            return t;
        };
    }


    /// @param argument extra argument, the value will prepend at zero position.
    static <T> Function<T, T> log(BiConsumer<String, Object[]> log, String pattern, Object... argument) {
        return v -> {
            log.accept(pattern, argument.length == 0 ? new Object[]{v} : Many.prepend(argument, v));
            return v;
        };
    }

    static <T> Function<T, T> log(boolean cond, BiConsumer<String, Object[]> log, String pattern, Object... argument) {
        if (!cond) return Function.identity();
        return v -> {
            log.accept(pattern, argument.length == 0 ? new Object[]{v} : Many.prepend(argument, v));
            return v;
        };
    }


    static Function<Boolean, Void> trueValue(Supplier<DomainError> err) {
        return v -> {
            if (v == null || !v) throw err.get();
            return null;
        };
    }


    //endregion
    record Pair<A, B>(A a, B b) {
        public static <A, B> Function<Pair<A, B>, A> predicate1(BiPredicate<? super A, ? super B> act, Supplier<DomainError> err) {
            return p -> {
                if (act.test(p.a, p.b)) return p.a;
                throw err.get();
            };
        }

        public static <A, B> Function<Pair<A, B>, A> predicate1(BiPredicate<? super A, ? super B> act, BiFunction<A, B, DomainError> err) {
            return p -> {
                if (act.test(p.a, p.b)) return p.a;
                throw err.apply(p.a, p.b);
            };
        }

        public static <A, B> Function<Pair<A, B>, B> predicate2(BiPredicate<? super A, ? super B> act, Supplier<DomainError> err) {
            return p -> {
                if (act.test(p.a, p.b)) return p.b;
                throw err.get();
            };
        }

        public static <A, B> Function<Pair<A, B>, B> predicate2(BiPredicate<? super A, ? super B> act, BiFunction<A, B, DomainError> err) {
            return p -> {
                if (act.test(p.a, p.b)) return p.b;
                throw err.apply(p.a, p.b);
            };
        }
    }

    interface Flat {
        static <I, O> Function<I, Future<Pair<I, O>>> concat(Function<I, Future<O>> action) {
            return i -> action.apply(i).map(o -> new Pair<>(i, o));
        }

        /// Mapping input I to combined value O
        static <I, O, V> Function<I, Future<O>> pipe(Supplier<Future<V>> act, Function2<I, V, O> set) {
            return u -> act.get().map(set.applyPartially(u));
        }

        /// Mapping input I with element I0 to value O0 and combine to O
        static <I, O, I0, O0> Function<I, Future<O>> pipe(Function<I, I0> get, Function<I0, Future<O0>> act, Function2<I, O0, O> set) {
            return u -> act.apply(get.apply(u)).map(set.applyPartially(u));
        }

        static <I, O, I0, O0> Function<I, Future<O>> pack(Function<I, I0> get, Function<I, Future<O0>> act, Function2<I0, O0, O> set) {
            return u -> act.apply(u).map(set.applyPartially(get.apply(u)));
        }


        static <T> Function<T, Future<T>> peek(Function<T, Future<?>> a) {
            return v -> a.apply(v).map(v);
        }

        static <T> Function<T, Future<T>> expect(Function<T, Future<Boolean>> a) {
            return v -> a.apply(v)
                    .map(x -> {
                        if (!x) throw new VertxException("Unexpected result: " + v, true);
                        return v;
                    });
        }

        //region futures


        interface ProvideFuture<T> extends Supplier<Future<T>> {
            Future<T> get();

            /**
             * @param cond condition that use next Provider
             * @param next next provider
             * @return chained FutureProvider
             */
            default ProvideFuture<T> next(Predicate<T> cond, ProvideFuture<T> next) {
                return () -> get().flatMap(v -> cond.test(v) ? next.get() : Future.succeededFuture(v));
            }

            /**
             * @param cond condition of throwable or value that should use next provider
             * @param next next providers
             * @return provider
             */
            default ProvideFuture<T> next(BiPredicate<@Nullable Throwable, @Nullable T> cond, ProvideFuture<T> next) {
                return () -> get()
                        .transform(
                                r -> r.failed() ? cond.test(r.cause(), null) ? next.get() : Future.failedFuture(r.cause())
                                        : cond.test(null, r.result()) ? next.get() : Future.succeededFuture(r.result()));
            }

            @SuppressWarnings("unchecked")
            default ProvideFuture<T> next(BiPredicate<@Nullable Throwable, @Nullable T> cond, ProvideFuture<T>... next) {
                if (next.length == 0) return this;
                var r = this;
                for (var n : next) {
                    r = r.next(cond, n);
                }
                return r;
            }
        }

        /// convert async vert.x future to synchronized invoke.**Note**: this may block event-loop.
        @SneakyThrows
        static <T> T sync(Future<T> async, long timeout, TimeUnit unit) {
            var f = async.toCompletionStage().toCompletableFuture();
            return f.get(timeout, unit);
        }

        /// convert async vert.x future to synchronized invoke.**Note**: this may block event-loop.
        @SneakyThrows
        static <T> T sync(Future<T> async) {
            return async.toCompletionStage().toCompletableFuture().get();
        }

        /// loop execution until result matches the end check condition.
        static <T> Future<T> loop(Predicate<T> endCheck, Supplier<Future<T>> action) {
            var h = loopHandler(endCheck, action);
            return action.get().transform(h);
        }

        private static <T> Function<AsyncResult<T>, Future<T>> loopHandler(Predicate<T> endCheck, Supplier<Future<T>> action) {
            return r -> {
                if (r.succeeded() && endCheck.test(r.result())) {
                    return Future.succeededFuture(r.result());
                } else {
                    return loop(endCheck, action);
                }
            };
        }

        /// loop execution until result matches the end check condition or timeout .
        static <T> Future<T> loopTimeout(Predicate<T> endCheck, Supplier<Future<T>> action, long timeout, TimeUnit unit) {
            return loop(endCheck, action).timeout(timeout, unit);

        }

        ///
        /// Retry with both delay and exception condition
        ///
        static <T> Future<T> retry(Vertx vertx, long wait, Supplier<Future<T>> action,
                                   Predicate<Throwable> cond, int times) {
            return retry(vertx, wait, action, cond, times, 0);
        }

        ///
        /// Fixed number of retries
        ///
        static <T> Future<T> retry(Vertx vertx, Supplier<Future<T>> action, int times) {
            return retry(vertx, 0, action, null, times, 0);
        }

        ///
        /// Retry with exception condition check
        ///
        static <T> Future<T> retry(Vertx vertx, Supplier<Future<T>> action, Predicate<Throwable> cond, int times) {
            return retry(vertx, 0, action, cond, times, 0);
        }

        ///
        /// Fixed retries with delay between attempts
        ///
        static <T> Future<T> retry(Vertx vertx, long wait, Supplier<Future<T>> action, int times) {
            return retry(vertx, wait, action, null, times, 0);
        }

        private static <T> Future<T> retry(Vertx vertx, long wait, Supplier<Future<T>> action, Predicate<Throwable> cond, int maxTimes, int attemptCount) {
            return action.get().recover(err -> {
                if (
                        err instanceof TimeoutException ||
                        attemptCount >= maxTimes ||
                        (cond != null && !cond.test(err))
                ) {
                    return Future.failedFuture(err);
                }
                if (vertx != null && wait > 0)
                    return vertx.timer(wait).compose(t -> retry(vertx, wait, action, cond, maxTimes, attemptCount + 1));
                return retry(vertx, wait, action, cond, maxTimes, attemptCount + 1);
            });
        }

        private static <T> boolean continuesOnFailure(@Nullable T $, @Nullable Throwable $$) {
            return $$ != null;
        }

        ///
        /// Iterate through futures until got a success result or all futures are exhausted
        ///
        static <T> Future<T> iterate(Supplier<Future<T>> iterSupplier) {
            return iterate(Flat::continuesOnFailure, iterSupplier);
        }

        ///
        /// Iterate through futures until condition is met or all futures are exhausted
        ///
        static <T> Future<T> iterate(
                BiPredicate<@Nullable T, @Nullable Throwable> continues,
                Supplier<Future<T>> iterSupplier
        ) {
            Future<T> nextFuture = iterSupplier.get();
            if (nextFuture == null) {
                return Future.failedFuture("No more futures to iterate");
            }
            return nextFuture.compose(
                    result -> handleSuccess(result, continues, iterSupplier),
                    failure -> handleFailure(failure, continues, iterSupplier)
            );
        }

        private static <T> Future<T> handleSuccess(
                T result,
                BiPredicate<@Nullable T, @Nullable Throwable> continues,
                Supplier<Future<T>> iterSupplier
        ) {
            try {
                if (continues.test(result, null)) return iterate(continues, iterSupplier);
                return Future.succeededFuture(result);
            } catch (Exception e) {
                return handleFailure(e, continues, iterSupplier);
            }
        }

        private static <T> Future<T> handleFailure(
                Throwable failure,
                BiPredicate<@Nullable T, @Nullable Throwable> continues,
                Supplier<Future<T>> iterSupplier
        ) {
            if (continues.test(null, failure)) return iterate(continues, iterSupplier);
            return Future.failedFuture(failure);
        }

        //endregion

    }

    interface Maybe {
        /// return value if can cast otherwise empty
        static <T, R> Function<Optional<T>, Optional<R>> cast(Class<R> type) {
            return t -> t.filter(type::isInstance).map(type::cast);
        }

        static <T> UnaryOperator<Optional<T>> filter(Predicate<T> a) {
            return v -> v.filter(a);
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static <T> Optional<T> notEmpty(Optional<T> v) {
            if (v.isPresent()) throw DomainError.System.conflict("alreadyExists");
            return v;
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static <T> T orElseNull(Optional<T> v) {
            return v.orElse(null);
        }


        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static boolean orElseFalse(Optional<Boolean> v) {
            return v.orElse(false);
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static boolean orElseTrue(Optional<Boolean> v) {
            return v.orElse(true);
        }

        static <T> UnaryOperator<Optional<T>> notEmpty(Supplier<DomainError> err) {
            return v -> {
                if (v.isPresent()) throw err.get();
                return v;
            };
        }

        static <T, R> Function<Optional<T>, Optional<R>> ifPresent(Function<T, R> act) {
            return v -> v.map(act);
        }

        static <T, R> Function<Optional<T>, Optional<R>> ifPresent(Function<T, R> act, Supplier<Optional<R>> orElse) {
            return v -> v.map(act).or(orElse);
        }
        static <T> UnaryOperator<Optional<T>> ifPresent(Consumer<? super T> act, Runnable orElse) {
            return v -> {
                if(v.isPresent()) act.accept(v.get());
                else orElse.run();
                return v;
            };
        }
        static <T> Function<Optional<T>, T> orElseThrow(Supplier<DomainError> err) {
            return v -> {
                if (v.isEmpty()) throw err.get();
                return v.get();
            };
        }

        static <T> Function<Optional<T>, T> orElse(T v) {
            return x -> x.orElse(v);
        }

        static <T> Function<Optional<T>, T> orElse(Supplier<T> v) {
            return x -> x.orElseGet(v);
        }

        interface Flat {
            static <I, O> Function<I, Future<Optional<O>>> optional(Function<I, Future<O>> act) {
                return i -> act.apply(i).map(Optional::ofNullable);
            }

            static <T, R> Function<Optional<T>, Future<Optional<R>>> isPresent(Function<T, Future<Optional<R>>> act) {
                return t -> {
                    if (t.isPresent()) return act.apply(t.get());
                    return Future.succeededFuture(Optional.empty());
                };
            }

            static <T> Function<Optional<T>, Future<Optional<T>>> isAbsent(Supplier<Future<Optional<T>>> act) {
                return t -> {
                    if (t.isPresent()) return Future.succeededFuture(t);
                    return act.get();
                };
            }

            static <T> Function<Optional<T>, Future<Optional<T>>> convert(
                    Function<T, Future<Optional<T>>> present,
                    Supplier<Future<Optional<T>>> absent
            ) {
                return t -> {
                    if (t.isPresent()) return present.apply(t.get());
                    return absent.get();
                };
            }
        }
    }

    interface Many {
        static <T> T[] append(T[] v, T t) {
            v = Arrays.copyOf(v, v.length + 1);
            v[v.length - 1] = t;
            return v;
        }

        static <T> T[] prepend(T[] v, T t) {
            @SuppressWarnings("unchecked")
            T[] copy = (T[]) Array.newInstance(v.getClass().getComponentType(), v.length + 1);
            System.arraycopy(v, 0, copy, 1, v.length);
            copy[0] = t;
            return copy;
        }

        static <T> T onlyFirst(Collection<? extends T> v) {
            if (v != null && v.size() == 1) return v.iterator().next();
            throw DomainError.System.conflict("duplicate or missing value");
        }

        static <K, V, M extends Map<?extends K, ?extends V>,R> Function<? extends M, List<List<R>>> nullSafeMapMapping(Function<? super K, ? extends R> km, Function< ? super V,  ? extends R> vm) {
            return t -> t == null ? null : t.entrySet()
                    .stream()
                    .map(e -> List.of(km.apply(e.getKey()), vm.apply(e.getValue())))
                    .toList();
        }

        static <V, M extends Collection<V>,R> Function<? super M, List<? extends R>> nullSafeCollectionMapping(Function<? super V, ? extends R> vm) {
            return t -> t == null ? null : t.stream().map(vm).toList();
        }


        static <T> Function<List<T>, List<T>> filter(Predicate<? super T> a) {
            return v -> v.stream().filter(a).toList();
        }

        static <T> Function<List<T>, List<T>> mutable() {
            return ArrayList::new;
        }

        static <T> Function<List<T>, List<T>> immutable() {
            return Collections::unmodifiableList;
        }

        static <T, R> Function<List<T>, R> reduce(Supplier<R> id, BiFunction< R, ? super T, R> acc, BinaryOperator<R> comb) {
            return t -> t.stream().reduce(id.get(), acc, comb);
        }

        static <T, K> Function<List<? extends T>, Map<? extends K, ? extends List<? extends T>>> group(Function<? super T, ? extends K> classifier) {
            return t -> t.stream().collect(Collectors.groupingBy(classifier));
        }

        static <T, K> Function<List<? extends T>, Map<? extends K, ? extends T>> toMap(Function<? super T, ? extends K> classifier) {
            return t -> t.stream().collect(Collectors.toMap(classifier, Function.identity()));
        }

        static <T, K> Map<? extends K, ? extends T> toMap(List<Tuple2<? extends K, ? extends T>> t) {
            return t.stream().collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
        }

        interface Flat {
            static <T, K, KS extends Collection<? extends K>> Future<List<? extends T>> all(KS ks, Function<? super K, ? extends Future<? extends T>> one) {
                return all(ks.stream().map(one).toList());
            }

            static <T> Function<List<? extends T>, Future<List<? extends T>>> filter(Function<? super T, Future<Boolean>> a) {
                return v -> Future.join(v.stream().map(x -> a.apply(x).map(s -> s ? x : null)).toList())
                        .map(x -> x.<T>list().stream().filter(Objects::nonNull).toList());
            }

            /// invoke future in sequence
            static Future<Void> seq(List<? extends Future<Void>> futures) {
                var result = Future.<Void>succeededFuture();
                for (var future : futures) {
                    result = result.compose(v -> future);
                }
                return result;
            }

            static Future<Void> seqSupplier(List<Supplier<Future<Void>>> futures) {
                var result = Future.<Void>succeededFuture();
                for (var future : futures) {
                    result = result.compose(v -> future.get());
                }
                return result;
            }

            static <T, K> Function<List<? extends T>, Future<Map<K, List<T>>>> group(Function<? super T, Future<? extends K>> classifier) {
                return t -> Future.join(t.stream()
                                .map(x -> classifier.apply(x).map(i -> Map.entry(i, x)))
                                .toList())
                        .map(CompositeFuture::<Map.Entry<K, T>>list)
                        .map(entries -> entries
                                .stream()
                                .collect(Collectors.groupingBy(
                                        Map.Entry::getKey,
                                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
                        );
            }

            /**
             * auto filtered for null value
             */
            static <T> Future<List<? extends T>> all(List<? extends Future<? extends T>> futures) {
                return Future.all(futures.stream().filter(Objects::nonNull).toList()).map(CompositeFuture::list);
            }

            /**
             * auto filtered for null value
             */
            static <T> Future<List<? extends T>> allFlatten(List<? extends Future<? extends List<? extends T>>> futures) {
                return Future.all(futures.stream().filter(Objects::nonNull).toList())
                        .map(CompositeFuture::<List<T>>list)
                        .map(v -> v.stream().flatMap(Collection::stream).toList());
            }

            /**
             * auto filtered for null value
             */
            static <T> Future<List<? extends T>> joinFlatten(List<? extends Future<List<? extends T>>> list) {
                return Future.join(list.stream().filter(Objects::nonNull).toList())
                        .map(CompositeFuture::<List<T>>list)
                        .map(s -> s.stream().flatMap(Collection::stream).toList());
            }

            /**
             * Auto filtered for null value
             */
            static <T> Future<List<? extends T>> join(List<? extends Future<? extends T>> list) {
                return Future.join(list.stream().filter(Objects::nonNull).toList()).map(CompositeFuture::list);
            }

            static <T> Future<List<Result<T>>> joinResultFlatten(List<? extends Future<List<? extends T>>> list) {
                return Future.join(list.stream().filter(Objects::nonNull).toList())
                        .transform(r -> {
                            var x = r.result();
                            var data = x.<List<T>>list();
                            if (r.succeeded())
                                return Future.succeededFuture(data.stream().flatMap(List::stream).map(Result::ok).toList());
                            return Future.succeededFuture(Seq.range(0, data.size())
                                    .map(i -> x.failed(i) ? List.of(Result.<T>fail(x.cause(i))) : data.get(i)
                                            .stream()
                                            .map(Result::ok)
                                            .toList())
                                    .flatMap(List::stream)
                                    .toList());
                        });
            }

            static <T> Future<List<Result<T>>> joinResult(List<? extends Future<? extends T>> list) {
                return Future.join(list.stream().filter(Objects::nonNull).toList())
                        .transform(r -> {
                            var x = r.result();
                            var data = x.<T>list();
                            if (r.succeeded())
                                return Future.succeededFuture(data.stream().map(Result::ok).toList());
                            return Future.succeededFuture(Seq.range(0, data.size())
                                    .map(i -> x.failed(i) ? Result.<T>fail(x.cause(i)) : Result.ok(data.get(i)))
                                    .toList());
                        });
            }

            //region CompositeFutures
            @SuppressWarnings("unchecked")
            static <V0, V1> Tuple2<V0, V1> collect2(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1));
            }

            static <V0, V1> Future<Tuple2<V0, V1>> all(Future<V0> v0, Future<V1> v1) {
                return Future.all(List.of(v0, v1)).map(Flat::collect2);
            }

            static <V0, V1> Future<Tuple2<V0, V1>> join(Future<V0> v0, Future<V1> v1) {
                return Future.join(List.of(v0, v1)).map(Flat::collect2);
            }

            static <V0, V1> Future<Tuple2<V0, V1>> any(Future<V0> v0, Future<V1> v1) {
                return Future.any(List.of(v0, v1)).map(Flat::collect2);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2> Tuple3<V0, V1, V2> collect3(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2));
            }

            static <V0, V1, V2> Future<Tuple3<V0, V1, V2>> all(Future<V0> v0, Future<V1> v1, Future<V2> v2) {
                return Future.all(List.of(v0, v1, v2)).map(Flat::collect3);
            }

            static <V0, V1, V2> Future<Tuple3<V0, V1, V2>> join(Future<V0> v0, Future<V1> v1, Future<V2> v2) {
                return Future.join(List.of(v0, v1, v2)).map(Flat::collect3);
            }

            static <V0, V1, V2> Future<Tuple3<V0, V1, V2>> any(Future<V0> v0, Future<V1> v1, Future<V2> v2) {
                return Future.any(List.of(v0, v1, v2)).map(Flat::collect3);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3> Tuple4<V0, V1, V2, V3> collect4(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3));
            }

            static <V0, V1, V2, V3> Future<Tuple4<V0, V1, V2, V3>> all(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                       Future<V3> v3) {
                return Future.all(List.of(v0, v1, v2, v3)).map(Flat::collect4);
            }

            static <V0, V1, V2, V3> Future<Tuple4<V0, V1, V2, V3>> join(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                        Future<V3> v3) {
                return Future.join(List.of(v0, v1, v2, v3)).map(Flat::collect4);
            }

            static <V0, V1, V2, V3> Future<Tuple4<V0, V1, V2, V3>> any(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                       Future<V3> v3) {
                return Future.any(List.of(v0, v1, v2, v3)).map(Flat::collect4);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4> Tuple5<V0, V1, V2, V3, V4> collect5(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4));
            }

            static <V0, V1, V2, V3, V4> Future<Tuple5<V0, V1, V2, V3, V4>> all(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                               Future<V3> v3, Future<V4> v4) {
                return Future.all(List.of(v0, v1, v2, v3, v4)).map(Flat::collect5);
            }

            static <V0, V1, V2, V3, V4> Future<Tuple5<V0, V1, V2, V3, V4>> join(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                                Future<V3> v3, Future<V4> v4) {
                return Future.join(List.of(v0, v1, v2, v3, v4)).map(Flat::collect5);
            }

            static <V0, V1, V2, V3, V4> Future<Tuple5<V0, V1, V2, V3, V4>> any(Future<V0> v0, Future<V1> v1, Future<V2> v2,
                                                                               Future<V3> v3, Future<V4> v4) {
                return Future.any(List.of(v0, v1, v2, v3, v4)).map(Flat::collect5);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5> Tuple6<V0, V1, V2, V3, V4, V5> collect6(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5));
            }

            static <V0, V1, V2, V3, V4, V5> Future<Tuple6<V0, V1, V2, V3, V4, V5>> all(Future<V0> v0, Future<V1> v1,
                                                                                       Future<V2> v2, Future<V3> v3,
                                                                                       Future<V4> v4, Future<V5> v5) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5)).map(Flat::collect6);
            }

            static <V0, V1, V2, V3, V4, V5> Future<Tuple6<V0, V1, V2, V3, V4, V5>> join(Future<V0> v0, Future<V1> v1,
                                                                                        Future<V2> v2, Future<V3> v3,
                                                                                        Future<V4> v4, Future<V5> v5) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5)).map(Flat::collect6);
            }

            static <V0, V1, V2, V3, V4, V5> Future<Tuple6<V0, V1, V2, V3, V4, V5>> any(Future<V0> v0, Future<V1> v1,
                                                                                       Future<V2> v2, Future<V3> v3,
                                                                                       Future<V4> v4, Future<V5> v5) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5)).map(Flat::collect6);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6> Tuple7<V0, V1, V2, V3, V4, V5, V6> collect7(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6));
            }

            static <V0, V1, V2, V3, V4, V5, V6> Future<Tuple7<V0, V1, V2, V3, V4, V5, V6>> all(Future<V0> v0, Future<V1> v1,
                                                                                               Future<V2> v2, Future<V3> v3,
                                                                                               Future<V4> v4, Future<V5> v5,
                                                                                               Future<V6> v6) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6)).map(Flat::collect7);
            }

            static <V0, V1, V2, V3, V4, V5, V6> Future<Tuple7<V0, V1, V2, V3, V4, V5, V6>> join(Future<V0> v0, Future<V1> v1,
                                                                                                Future<V2> v2, Future<V3> v3,
                                                                                                Future<V4> v4, Future<V5> v5,
                                                                                                Future<V6> v6) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6)).map(Flat::collect7);
            }

            static <V0, V1, V2, V3, V4, V5, V6> Future<Tuple7<V0, V1, V2, V3, V4, V5, V6>> any(Future<V0> v0, Future<V1> v1,
                                                                                               Future<V2> v2, Future<V3> v3,
                                                                                               Future<V4> v4, Future<V5> v5,
                                                                                               Future<V6> v6) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6)).map(Flat::collect7);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7> Tuple8<V0, V1, V2, V3, V4, V5, V6, V7> collect8(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7> Future<Tuple8<V0, V1, V2, V3, V4, V5, V6, V7>> all(Future<V0> v0,
                                                                                                       Future<V1> v1,
                                                                                                       Future<V2> v2,
                                                                                                       Future<V3> v3,
                                                                                                       Future<V4> v4,
                                                                                                       Future<V5> v5,
                                                                                                       Future<V6> v6,
                                                                                                       Future<V7> v7) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7)).map(Flat::collect8);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7> Future<Tuple8<V0, V1, V2, V3, V4, V5, V6, V7>> join(Future<V0> v0,
                                                                                                        Future<V1> v1,
                                                                                                        Future<V2> v2,
                                                                                                        Future<V3> v3,
                                                                                                        Future<V4> v4,
                                                                                                        Future<V5> v5,
                                                                                                        Future<V6> v6,
                                                                                                        Future<V7> v7) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7)).map(Flat::collect8);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7> Future<Tuple8<V0, V1, V2, V3, V4, V5, V6, V7>> any(Future<V0> v0,
                                                                                                       Future<V1> v1,
                                                                                                       Future<V2> v2,
                                                                                                       Future<V3> v3,
                                                                                                       Future<V4> v4,
                                                                                                       Future<V5> v5,
                                                                                                       Future<V6> v6,
                                                                                                       Future<V7> v7) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7)).map(Flat::collect8);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Tuple9<V0, V1, V2, V3, V4, V5, V6, V7, V8> collect9(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Future<Tuple9<V0, V1, V2, V3, V4, V5, V6, V7, V8>> all(Future<V0> v0,
                                                                                                               Future<V1> v1,
                                                                                                               Future<V2> v2,
                                                                                                               Future<V3> v3,
                                                                                                               Future<V4> v4,
                                                                                                               Future<V5> v5,
                                                                                                               Future<V6> v6,
                                                                                                               Future<V7> v7,
                                                                                                               Future<V8> v8) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8)).map(Flat::collect9);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Future<Tuple9<V0, V1, V2, V3, V4, V5, V6, V7, V8>> join(Future<V0> v0,
                                                                                                                Future<V1> v1,
                                                                                                                Future<V2> v2,
                                                                                                                Future<V3> v3,
                                                                                                                Future<V4> v4,
                                                                                                                Future<V5> v5,
                                                                                                                Future<V6> v6,
                                                                                                                Future<V7> v7,
                                                                                                                Future<V8> v8) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8)).map(Flat::collect9);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Future<Tuple9<V0, V1, V2, V3, V4, V5, V6, V7, V8>> any(Future<V0> v0,
                                                                                                               Future<V1> v1,
                                                                                                               Future<V2> v2,
                                                                                                               Future<V3> v3,
                                                                                                               Future<V4> v4,
                                                                                                               Future<V5> v5,
                                                                                                               Future<V6> v6,
                                                                                                               Future<V7> v7,
                                                                                                               Future<V8> v8) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8)).map(Flat::collect9);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Tuple10<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> collect10(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Future<Tuple10<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9)).map(Flat::collect10);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Future<Tuple10<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9)).map(Flat::collect10);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Future<Tuple10<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9)).map(Flat::collect10);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Tuple11<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> collect11(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Future<Tuple11<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)).map(Flat::collect11);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Future<Tuple11<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)).map(Flat::collect11);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Future<Tuple11<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)).map(Flat::collect11);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Tuple12<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> collect12(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10), (V11) l.get(11));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Future<Tuple12<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)).map(Flat::collect12);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Future<Tuple12<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)).map(Flat::collect12);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Future<Tuple12<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)).map(Flat::collect12);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Tuple13<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> collect13(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10), (V11) l.get(11),
                        (V12) l.get(12));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Future<Tuple13<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)).map(Flat::collect13);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Future<Tuple13<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)).map(Flat::collect13);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Future<Tuple13<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)).map(Flat::collect13);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Tuple14<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> collect14(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10), (V11) l.get(11),
                        (V12) l.get(12), (V13) l.get(13));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Future<Tuple14<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)).map(Flat::collect14);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Future<Tuple14<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)).map(Flat::collect14);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Future<Tuple14<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)).map(Flat::collect14);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Tuple15<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> collect15(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10), (V11) l.get(11),
                        (V12) l.get(12), (V13) l.get(13), (V14) l.get(14));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Future<Tuple15<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14))
                        .map(Flat::collect15);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Future<Tuple15<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14))
                        .map(Flat::collect15);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Future<Tuple15<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14))
                        .map(Flat::collect15);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Tuple16<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> collect16(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple((V0) l.get(0), (V1) l.get(1), (V2) l.get(2), (V3) l.get(3), (V4) l.get(4), (V5) l.get(5),
                        (V6) l.get(6), (V7) l.get(7), (V8) l.get(8), (V9) l.get(9), (V10) l.get(10), (V11) l.get(11),
                        (V12) l.get(12), (V13) l.get(13), (V14) l.get(14), (V15) l.get(15));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Future<Tuple16<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15>> all(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14, Future<V15> v15) {
                return Future.all(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15))
                        .map(Flat::collect16);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Future<Tuple16<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15>> join(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14, Future<V15> v15) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15))
                        .map(Flat::collect16);
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Future<Tuple16<V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15>> any(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14, Future<V15> v15) {
                return Future.any(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15))
                        .map(Flat::collect16);
            }

            //endregion

            //region JoinResult
            @SuppressWarnings("unchecked")
            static <V0, V1> Tuple2<Result<V0>, Result<V1>> collectResult2(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)));
            }

            static <V0, V1> Future<Tuple2<Result<V0>, Result<V1>>> joinResult(Future<V0> v0, Future<V1> v1) {
                return Future.join(List.of(v0, v1)).map(Flat::collectResult2);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2> Tuple3<Result<V0>, Result<V1>, Result<V2>> collectResult3(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)));
            }

            static <V0, V1, V2> Future<Tuple3<Result<V0>, Result<V1>, Result<V2>>> joinResult(Future<V0> v0, Future<V1> v1,
                                                                                              Future<V2> v2) {
                return Future.join(List.of(v0, v1, v2)).map(Flat::collectResult3);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3> Tuple4<Result<V0>, Result<V1>, Result<V2>, Result<V3>> collectResult4(CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)));
            }

            static <V0, V1, V2, V3> Future<Tuple4<Result<V0>, Result<V1>, Result<V2>, Result<V3>>> joinResult(Future<V0> v0,
                                                                                                              Future<V1> v1,
                                                                                                              Future<V2> v2,
                                                                                                              Future<V3> v3) {
                return Future.join(List.of(v0, v1, v2, v3)).map(Flat::collectResult4);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4> Tuple5<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>> collectResult5(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)));
            }

            static <V0, V1, V2, V3, V4> Future<Tuple5<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4) {
                return Future.join(List.of(v0, v1, v2, v3, v4)).map(Flat::collectResult5);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5> Tuple6<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>> collectResult6(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)));
            }

            static <V0, V1, V2, V3, V4, V5> Future<Tuple6<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5)).map(Flat::collectResult6);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6> Tuple7<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>> collectResult7(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)));
            }

            static <V0, V1, V2, V3, V4, V5, V6> Future<Tuple7<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6)).map(Flat::collectResult7);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7> Tuple8<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>> collectResult8(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7> Future<Tuple8<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7)).map(Flat::collectResult8);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Tuple9<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>> collectResult9(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8> Future<Tuple9<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8)).map(Flat::collectResult9);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Tuple10<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>> collectResult10(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9> Future<Tuple10<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9)).map(Flat::collectResult10);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Tuple11<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>> collectResult11(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10> Future<Tuple11<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)).map(Flat::collectResult11);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Tuple12<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>> collectResult12(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)),
                        v.succeeded(11) ? Result.ok((V11) l.get(11)) : Result.fail(v.cause(11)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11> Future<Tuple12<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)).map(Flat::collectResult12);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Tuple13<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>> collectResult13(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)),
                        v.succeeded(11) ? Result.ok((V11) l.get(11)) : Result.fail(v.cause(11)),
                        v.succeeded(12) ? Result.ok((V12) l.get(12)) : Result.fail(v.cause(12)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12> Future<Tuple13<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12))
                        .map(Flat::collectResult13);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Tuple14<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>> collectResult14(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)),
                        v.succeeded(11) ? Result.ok((V11) l.get(11)) : Result.fail(v.cause(11)),
                        v.succeeded(12) ? Result.ok((V12) l.get(12)) : Result.fail(v.cause(12)),
                        v.succeeded(13) ? Result.ok((V13) l.get(13)) : Result.fail(v.cause(13)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> Future<Tuple14<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13))
                        .map(Flat::collectResult14);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Tuple15<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>, Result<V14>> collectResult15(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)),
                        v.succeeded(11) ? Result.ok((V11) l.get(11)) : Result.fail(v.cause(11)),
                        v.succeeded(12) ? Result.ok((V12) l.get(12)) : Result.fail(v.cause(12)),
                        v.succeeded(13) ? Result.ok((V13) l.get(13)) : Result.fail(v.cause(13)),
                        v.succeeded(14) ? Result.ok((V14) l.get(14)) : Result.fail(v.cause(14)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> Future<Tuple15<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>, Result<V14>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14))
                        .map(Flat::collectResult15);
            }

            @SuppressWarnings("unchecked")
            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Tuple16<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>, Result<V14>, Result<V15>> collectResult16(
                    CompositeFuture v) {
                var l = v.list();
                return Tuple.tuple(v.succeeded(0) ? Result.ok((V0) l.get(0)) : Result.fail(v.cause(0)),
                        v.succeeded(1) ? Result.ok((V1) l.get(1)) : Result.fail(v.cause(1)),
                        v.succeeded(2) ? Result.ok((V2) l.get(2)) : Result.fail(v.cause(2)),
                        v.succeeded(3) ? Result.ok((V3) l.get(3)) : Result.fail(v.cause(3)),
                        v.succeeded(4) ? Result.ok((V4) l.get(4)) : Result.fail(v.cause(4)),
                        v.succeeded(5) ? Result.ok((V5) l.get(5)) : Result.fail(v.cause(5)),
                        v.succeeded(6) ? Result.ok((V6) l.get(6)) : Result.fail(v.cause(6)),
                        v.succeeded(7) ? Result.ok((V7) l.get(7)) : Result.fail(v.cause(7)),
                        v.succeeded(8) ? Result.ok((V8) l.get(8)) : Result.fail(v.cause(8)),
                        v.succeeded(9) ? Result.ok((V9) l.get(9)) : Result.fail(v.cause(9)),
                        v.succeeded(10) ? Result.ok((V10) l.get(10)) : Result.fail(v.cause(10)),
                        v.succeeded(11) ? Result.ok((V11) l.get(11)) : Result.fail(v.cause(11)),
                        v.succeeded(12) ? Result.ok((V12) l.get(12)) : Result.fail(v.cause(12)),
                        v.succeeded(13) ? Result.ok((V13) l.get(13)) : Result.fail(v.cause(13)),
                        v.succeeded(14) ? Result.ok((V14) l.get(14)) : Result.fail(v.cause(14)),
                        v.succeeded(15) ? Result.ok((V15) l.get(15)) : Result.fail(v.cause(15)));
            }

            static <V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15> Future<Tuple16<Result<V0>, Result<V1>, Result<V2>, Result<V3>, Result<V4>, Result<V5>, Result<V6>, Result<V7>, Result<V8>, Result<V9>, Result<V10>, Result<V11>, Result<V12>, Result<V13>, Result<V14>, Result<V15>>> joinResult(
                    Future<V0> v0, Future<V1> v1, Future<V2> v2, Future<V3> v3, Future<V4> v4, Future<V5> v5, Future<V6> v6,
                    Future<V7> v7, Future<V8> v8, Future<V9> v9, Future<V10> v10, Future<V11> v11, Future<V12> v12,
                    Future<V13> v13, Future<V14> v14, Future<V15> v15) {
                return Future.join(List.of(v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15))
                        .map(Flat::collectResult16);
            }

            //endregion
        }
    }

    sealed interface Result<T> {
        T value();

        record failure<T>(Throwable error) implements Result<T> {
            @SneakyThrows
            @Override
            public T value() {
                throw error;
            }
        }

        default @Nullable Throwable error() {
            return null;
        }

        record success<T>(T value) implements Result<T> {
        }

        static <T> Result<T> ok(T v) {
            return new success<>(v);
        }

        static <T> Result<T> fail(Throwable v) {
            return new failure<>(v);
        }

        default Future<T> future() {
            return error() instanceof Throwable ex ? Future.failedFuture(ex) : Future.succeededFuture(value());
        }

        default Optional<T> optional() {
            return error() != null ? Optional.empty() : Optional.ofNullable(value());
        }

        default <R> Result<R> apply(Function<T, R> mapper) {
            var ex = error();
            if (ex != null) return fail(ex);
            try {
                return ok(mapper.apply(value()));
            } catch (Throwable e) {
                return fail(e);
            }
        }
    }


    interface Primitives {
        static IntUnaryOperator predicate(IntPredicate cond, IntUnaryOperator onTrue, IntUnaryOperator onFalse) {
            return t -> cond.test(t) ? onTrue.applyAsInt(t) : onFalse.applyAsInt(t);
        }

        static IntUnaryOperator condition(BooleanSupplier cond, IntUnaryOperator onTrue, IntUnaryOperator onFalse) {
            return t -> cond.getAsBoolean() ? onTrue.applyAsInt(t) : onFalse.applyAsInt(t);
        }

        static LongUnaryOperator predicate(LongPredicate cond, LongUnaryOperator onTrue, LongUnaryOperator onFalse) {
            return t -> cond.test(t) ? onTrue.applyAsLong(t) : onFalse.applyAsLong(t);
        }

        static LongUnaryOperator condition(BooleanSupplier cond, LongUnaryOperator onTrue, LongUnaryOperator onFalse) {
            return t -> cond.getAsBoolean() ? onTrue.applyAsLong(t) : onFalse.applyAsLong(t);
        }

        static DoubleUnaryOperator predicate(DoublePredicate cond, DoubleUnaryOperator onTrue, DoubleUnaryOperator onFalse) {
            return t -> cond.test(t) ? onTrue.applyAsDouble(t) : onFalse.applyAsDouble(t);
        }

        static DoubleUnaryOperator condition(BooleanSupplier cond, DoubleUnaryOperator onTrue, DoubleUnaryOperator onFalse) {
            return t -> cond.getAsBoolean() ? onTrue.applyAsDouble(t) : onFalse.applyAsDouble(t);
        }

        static IntUnaryOperator peek(IntConsumer o) {
            return i -> {
                o.accept(i);
                return i;
            };
        }

        static LongUnaryOperator peek(LongConsumer o) {
            return i -> {
                o.accept(i);
                return i;
            };
        }

        static DoubleUnaryOperator peek(DoubleConsumer o) {
            return i -> {
                o.accept(i);
                return i;
            };
        }

        static IntUnaryOperator expect(Supplier<DomainError> err, IntPredicate o) {
            return i -> {
                if (!o.test(i)) throw err.get();
                return i;
            };
        }

        static LongUnaryOperator expect(Supplier<DomainError> err, LongPredicate o) {
            return i -> {
                if (!o.test(i)) throw err.get();
                return i;
            };
        }

        static DoubleUnaryOperator expect(Supplier<DomainError> err, DoublePredicate o) {
            return i -> {
                if (!o.test(i)) throw err.get();
                return i;
            };
        }

        static <I> IntSupplier supplierClosure(I v, ToIntFunction<I> value) {
            return () -> value.applyAsInt(v);
        }

        static <I> IntSupplier supplier(Supplier<I> v, ToIntFunction<I> value) {
            return () -> value.applyAsInt(v.get());
        }

        static <I> DoubleSupplier supplierClosure(I v, ToDoubleFunction<I> value) {
            return () -> value.applyAsDouble(v);
        }

        static <I> DoubleSupplier supplier(Supplier<I> v, ToDoubleFunction<I> value) {
            return () -> value.applyAsDouble(v.get());
        }

        static <I> LongSupplier supplierClosure(I v, ToLongFunction<I> value) {
            return () -> value.applyAsLong(v);
        }

        static <I> LongSupplier supplier(Supplier<I> v, ToLongFunction<I> value) {
            return () -> value.applyAsLong(v.get());
        }

        interface IntToCharFunction {
            char applyAsChar(int v);
        }

        IntToCharFunction INT_TO_CHAR = v -> (char) v;

        interface IntToByteFunction {
            byte applyAsByte(int v);
        }

        IntToByteFunction INT_TO_BYTE = v -> (byte) v;

        interface IntToShortFunction {
            short applyAsShort(int v);
        }

        IntToShortFunction INT_TO_SHORT = v -> (short) v;

        interface CharToIntFunction {
            int applyAsInt(char v);
        }

        CharToIntFunction CHAR_TO_INT = v -> v;

        interface ByteToIntFunction {
            int applyAsInt(byte v);
        }

        ByteToIntFunction BYTE_TO_INT = v -> v;

        interface ShortToIntFunction {
            int applyAsInt(short v);
        }

        ShortToIntFunction SHORT_TO_INT = v -> v;

        LongBinaryOperator LONG_BITWISE_SHL = (v, i) -> v << i;
        LongBinaryOperator LONG_BITWISE_SHR = (v, i) -> v >> i;
        LongBinaryOperator LONG_BITWISE_USHR = (v, i) -> v >>> i;

        LongBinaryOperator LONG_BITWISE_AND = (v0, v1) -> v0 & v1;
        LongBinaryOperator LONG_BITWISE_OR = (v0, v1) -> v0 | v1;
        LongBinaryOperator LONG_BITWISE_XOR = (v0, v1) -> v0 ^ v1;
        LongUnaryOperator LONG_BITWISE_NOT = i -> ~i;

        IntBinaryOperator INT_BITWISE_SHL = (v, i) -> v << i;
        IntBinaryOperator INT_BITWISE_SHR = (v, i) -> v >> i;
        IntBinaryOperator INT_BITWISE_USHR = (v, i) -> v >>> i;

        IntBinaryOperator INT_BITWISE_AND = (v0, v1) -> v0 & v1;
        IntBinaryOperator INT_BITWISE_OR = (v0, v1) -> v0 | v1;
        IntBinaryOperator INT_BITWISE_XOR = (v0, v1) -> v0 ^ v1;
        IntUnaryOperator INT_BITWISE_NOT = i -> ~i;

        @FunctionalInterface
        interface ByteBinaryOperator {
            byte applyAsByte(byte left, byte right);
        }

        @FunctionalInterface
        interface ByteUnaryOperator {
            byte applyAsByte(byte operand);

            default ByteUnaryOperator compose(ByteUnaryOperator before) {
                Objects.requireNonNull(before);
                return (byte v) -> applyAsByte(before.applyAsByte(v));
            }

            default ByteUnaryOperator andThen(ByteUnaryOperator after) {
                Objects.requireNonNull(after);
                return (byte t) -> after.applyAsByte(applyAsByte(t));
            }

            static DoubleUnaryOperator identity() {
                return t -> t;
            }
        }

        @FunctionalInterface
        interface ByteSupplier {
            byte getAsByte();
        }

        @FunctionalInterface
        interface ByteConsumer {
            void accept(byte value);

            default ByteConsumer andThen(ByteConsumer after) {
                Objects.requireNonNull(after);
                return (byte t) -> {
                    accept(t);
                    after.accept(t);
                };
            }
        }

        @FunctionalInterface
        interface BytePredicate {
            boolean test(byte value);

            default BytePredicate and(BytePredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) && other.test(value);
            }

            default BytePredicate negate() {
                return (value) -> !test(value);
            }

            default BytePredicate or(BytePredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) || other.test(value);
            }
        }

        @FunctionalInterface
        interface ByteFunction<R> {
            R apply(byte value);
        }

        @FunctionalInterface
        interface ToByteBiFunction<T, U> {
            byte applyAsByte(T t, U u);
        }

        @FunctionalInterface
        interface ToByteFunction<T> {
            byte applyAsByte(T value);
        }

        @FunctionalInterface
        interface ShortBinaryOperator {
            short applyAsShort(short left, short right);
        }

        @FunctionalInterface
        interface ShortUnaryOperator {
            short applyAsShort(short operand);

            default ShortUnaryOperator compose(ShortUnaryOperator before) {
                Objects.requireNonNull(before);
                return (short v) -> applyAsShort(before.applyAsShort(v));
            }

            default ShortUnaryOperator andThen(ShortUnaryOperator after) {
                Objects.requireNonNull(after);
                return (short t) -> after.applyAsShort(applyAsShort(t));
            }

            static DoubleUnaryOperator identity() {
                return t -> t;
            }
        }

        @FunctionalInterface
        interface ShortSupplier {
            short getAsShort();
        }

        @FunctionalInterface
        interface ShortConsumer {
            void accept(short value);

            default ShortConsumer andThen(ShortConsumer after) {
                Objects.requireNonNull(after);
                return (short t) -> {
                    accept(t);
                    after.accept(t);
                };
            }
        }

        @FunctionalInterface
        interface ShortPredicate {
            boolean test(short value);

            default ShortPredicate and(ShortPredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) && other.test(value);
            }

            default ShortPredicate negate() {
                return (value) -> !test(value);
            }

            default ShortPredicate or(ShortPredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) || other.test(value);
            }
        }

        @FunctionalInterface
        interface ShortFunction<R> {
            R apply(short value);
        }

        @FunctionalInterface
        interface ToShortBiFunction<T, U> {
            short applyAsShort(T t, U u);
        }

        @FunctionalInterface
        interface ToShortFunction<T> {
            short applyAsShort(T value);
        }

        @FunctionalInterface
        interface CharBinaryOperator {
            char applyAsChar(char left, char right);
        }

        @FunctionalInterface
        interface CharUnaryOperator {
            char applyAsChar(char operand);

            default CharUnaryOperator compose(CharUnaryOperator before) {
                Objects.requireNonNull(before);
                return (char v) -> applyAsChar(before.applyAsChar(v));
            }

            default CharUnaryOperator andThen(CharUnaryOperator after) {
                Objects.requireNonNull(after);
                return (char t) -> after.applyAsChar(applyAsChar(t));
            }

            static DoubleUnaryOperator identity() {
                return t -> t;
            }
        }

        @FunctionalInterface
        interface CharSupplier {
            char getAsChar();
        }

        @FunctionalInterface
        interface CharConsumer {
            void accept(char value);

            default CharConsumer andThen(CharConsumer after) {
                Objects.requireNonNull(after);
                return (char t) -> {
                    accept(t);
                    after.accept(t);
                };
            }
        }

        @FunctionalInterface
        interface CharPredicate {
            boolean test(char value);

            default CharPredicate and(CharPredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) && other.test(value);
            }

            default CharPredicate negate() {
                return (value) -> !test(value);
            }

            default CharPredicate or(CharPredicate other) {
                Objects.requireNonNull(other);
                return (value) -> test(value) || other.test(value);
            }
        }

        @FunctionalInterface
        interface CharFunction<R> {
            R apply(char value);
        }

        @FunctionalInterface
        interface ToCharBiFunction<T, U> {
            char applyAsChar(T t, U u);
        }

        @FunctionalInterface
        interface ToCharFunction<T> {
            char applyAsChar(T value);
        }


    }

    sealed interface Monad<T> {
        Monad<T> expect(Predicate<T> cond, Supplier<DomainError> err);

        Monad<T> peek(Consumer<T> op);

        Monad<T> empty(Predicate<T> op);

        static <T> Operation<T> operator() {
            return new Operation<>(Function.identity());
        }


        record Operation<T>(Function<T, T> value) implements Monad<T> {
            public Operation<T> expect(Predicate<T> cond, Supplier<DomainError> err) {
                return new Operation<>(value.andThen(Fn.expect(err, cond)));
            }

            public Operation<T> peek(Consumer<T> op) {
                return new Operation<>(value.andThen(Fn.peek(op)));
            }

            public <R> Process<T, R> map(Function<T, R> op) {
                return new Process<>(value.andThen(op));
            }

            @Override
            public Operation<T> empty(Predicate<T> op) {
                return new Operation<>(value.andThen(Fn.empty(op)));
            }

            public Operation<T> accept(boolean cond, Consumer<T> act) {
                return cond ? new Operation<>(value.andThen(Fn.peek(act))) : this;
            }

            public Operation<T> apply(boolean cond, UnaryOperator<T> act) {
                return cond ? new Operation<>(value.andThen(act)) : this;
            }

            public T apply(T value) {
                return this.value.apply(value);
            }

            public Supplier<T> supplier(T value) {
                return Fn.supplierClosure(value, this.value);
            }

            public Supplier<T> supplier(Supplier<T> value) {
                return Fn.supplier(value, this.value);
            }
        }

        static <I, R> Process<I, R> func(Function<I, R> fn) {
            return new Process<>(fn);
        }

        record Process<I, R>(Function<I, R> value) implements Monad<R> {
            public Process<I, R> condition(BooleanSupplier cond, UnaryOperator<R> onTrue, UnaryOperator<R> onFalse) {
                return new Process<>(value.andThen(Fn.condition(cond, onTrue, onFalse)));
            }

            public Process<I, R> predicate(Predicate<R> cond, UnaryOperator<R> onTrue, UnaryOperator<R> onFalse) {
                return new Process<>(value.andThen(Fn.predicate(cond, onTrue, onFalse)));
            }

            public Process<I, R> expect(Predicate<R> cond, Supplier<DomainError> err) {
                return new Process<>(value.andThen(Fn.expect(err, cond)));
            }

            public Process<I, R> peek(Consumer<R> op) {
                return new Process<>(value.andThen(Fn.peek(op)));
            }

            @Override
            public Process<I, R> empty(Predicate<R> op) {
                return new Process<>(value.andThen(Fn.empty(op)));
            }

            public <R1> Process<I, R1> map(Function<R, R1> op) {
                return new Process<>(value.andThen(op));
            }

            public ToIntProcess<I> mapToInt(ToIntFunction<R> op) {
                return new ToIntProcess<>(i -> op.applyAsInt(value.apply(i)));
            }

            public ToLongProcess<I> mapToLong(ToLongFunction<R> op) {
                return new ToLongProcess<>(i -> op.applyAsLong(value.apply(i)));
            }

            public ToDoubleProcess<I> mapToDouble(ToDoubleFunction<R> op) {
                return new ToDoubleProcess<>(i -> op.applyAsDouble(value.apply(i)));
            }

            public R apply(I value) {
                return this.value.apply(value);
            }

            public Supplier<R> supplier(I value) {
                return Fn.supplierClosure(value, this.value);
            }

            public Supplier<R> supplier(Supplier<I> value) {
                return Fn.supplier(value, this.value);
            }
        }

        static <I> ToIntProcess<I> processInt(ToIntFunction<I> s) {
            return new ToIntProcess<>(s);
        }

        record ToIntProcess<I>(ToIntFunction<I> value) {
            public ToIntProcess<I> condition(BooleanSupplier cond, IntUnaryOperator onTrue, IntUnaryOperator onFalse) {
                return new ToIntProcess<>(i -> {
                    var v = value.applyAsInt(i);
                    return cond.getAsBoolean() ? onTrue.applyAsInt(v) : onFalse.applyAsInt(v);
                });
            }

            public <R> Process<I, R> predicate(IntPredicate cond, IntFunction<R> onTrue, IntFunction<R> onFalse) {
                return new Process<>(i -> {
                    var v = value.applyAsInt(i);
                    return cond.test(v) ? onTrue.apply(v) : onFalse.apply(v);
                });
            }

            public ToIntProcess<I> expect(IntPredicate cond, Supplier<DomainError> err) {
                var x = Primitives.expect(err, cond);
                return new ToIntProcess<>(i -> {
                    var v = value.applyAsInt(i);
                    return x.applyAsInt(v);
                });
            }

            public ToIntProcess<I> peek(IntConsumer op) {
                return new ToIntProcess<>(i -> {
                    var v = value.applyAsInt(i);
                    op.accept(v);
                    return v;
                });
            }

            public ToIntProcess<I> map(IntUnaryOperator op) {
                return new ToIntProcess<>(i -> {
                    var v = value.applyAsInt(i);
                    return op.applyAsInt(v);
                });
            }

            public ToIntProcess<I> compute(IntSupplier first, IntBinaryOperator op) {
                return new ToIntProcess<>(i -> {
                    var v = value.applyAsInt(i);
                    return op.applyAsInt(first.getAsInt(), v);
                });
            }


            public <R> Process<I, R> map(IntFunction<R> op) {
                return new Process<>(i -> op.apply(value.applyAsInt(i)));
            }

            public int apply(I value) {
                return this.value.applyAsInt(value);
            }

            public IntSupplier supplier(I value) {
                return Primitives.supplierClosure(value, this.value);
            }

            public IntSupplier supplier(Supplier<I> value) {
                return Primitives.supplier(value, this.value);
            }
        }

        static <I> ToLongProcess<I> processLong(ToLongFunction<I> s) {
            return new ToLongProcess<>(s);
        }

        record ToLongProcess<I>(ToLongFunction<I> value) {
            public ToLongProcess<I> condition(BooleanSupplier cond, LongUnaryOperator onTrue, LongUnaryOperator onFalse) {
                return new ToLongProcess<>(i -> {
                    var v = value.applyAsLong(i);
                    return cond.getAsBoolean() ? onTrue.applyAsLong(v) : onFalse.applyAsLong(v);
                });
            }

            public <R> Process<I, R> predicate(LongPredicate cond, LongFunction<R> onTrue, LongFunction<R> onFalse) {
                return new Process<>(i -> {
                    var v = value.applyAsLong(i);
                    return cond.test(v) ? onTrue.apply(v) : onFalse.apply(v);
                });
            }

            public ToLongProcess<I> expect(LongPredicate cond, Supplier<DomainError> err) {
                var x = Primitives.expect(err, cond);
                return new ToLongProcess<>(i -> {
                    var v = value.applyAsLong(i);
                    return x.applyAsLong(v);
                });
            }

            public ToLongProcess<I> peek(LongConsumer op) {
                return new ToLongProcess<>(i -> {
                    var v = value.applyAsLong(i);
                    op.accept(v);
                    return v;
                });
            }

            public ToLongProcess<I> map(LongUnaryOperator op) {
                return new ToLongProcess<>(i -> {
                    var v = value.applyAsLong(i);
                    return op.applyAsLong(v);
                });
            }

            public ToLongProcess<I> compute(LongSupplier first, LongBinaryOperator op) {
                return new ToLongProcess<>(i -> {
                    var v = value.applyAsLong(i);
                    return op.applyAsLong(first.getAsLong(), v);
                });
            }

            public long apply(I value) {
                return this.value.applyAsLong(value);
            }

            public <R> Process<I, R> map(LongFunction<R> op) {
                return new Process<>(i -> op.apply(value.applyAsLong(i)));
            }

            public LongSupplier supplier(I value) {
                return Primitives.supplierClosure(value, this.value);
            }

            public LongSupplier supplier(Supplier<I> value) {
                return Primitives.supplier(value, this.value);
            }
        }

        static <I> ToDoubleProcess<I> processDouble(ToDoubleFunction<I> s) {
            return new ToDoubleProcess<>(s);
        }

        record ToDoubleProcess<I>(ToDoubleFunction<I> value) {
            public ToDoubleProcess<I> condition(BooleanSupplier cond, DoubleUnaryOperator onTrue, DoubleUnaryOperator onFalse) {
                return new ToDoubleProcess<>(i -> {
                    var v = value.applyAsDouble(i);
                    return cond.getAsBoolean() ? onTrue.applyAsDouble(v) : onFalse.applyAsDouble(v);
                });
            }

            public <R> Process<I, R> predicate(DoublePredicate cond, DoubleFunction<R> onTrue, DoubleFunction<R> onFalse) {
                return new Process<>(i -> {
                    var v = value.applyAsDouble(i);
                    return cond.test(v) ? onTrue.apply(v) : onFalse.apply(v);
                });
            }

            public ToDoubleProcess<I> expect(DoublePredicate cond, Supplier<DomainError> err) {
                var x = Primitives.expect(err, cond);
                return new ToDoubleProcess<>(i -> {
                    var v = value.applyAsDouble(i);
                    return x.applyAsDouble(v);
                });
            }

            public ToDoubleProcess<I> peek(DoubleConsumer op) {
                return new ToDoubleProcess<>(i -> {
                    var v = value.applyAsDouble(i);
                    op.accept(v);
                    return v;
                });
            }

            public ToDoubleProcess<I> map(DoubleUnaryOperator op) {
                return new ToDoubleProcess<>(i -> {
                    var v = value.applyAsDouble(i);
                    return op.applyAsDouble(v);
                });
            }

            public ToDoubleProcess<I> compute(DoubleSupplier first, DoubleBinaryOperator op) {
                return new ToDoubleProcess<>(i -> {
                    var v = value.applyAsDouble(i);
                    return op.applyAsDouble(first.getAsDouble(), v);
                });
            }


            public double apply(I value) {
                return this.value.applyAsDouble(value);
            }

            public <R> Process<I, R> map(DoubleFunction<R> op) {
                return new Process<>(i -> op.apply(value.applyAsDouble(i)));
            }

            public DoubleSupplier supplier(I value) {
                return Primitives.supplierClosure(value, this.value);
            }

            public DoubleSupplier supplier(Supplier<I> value) {
                return Primitives.supplier(value, this.value);
            }
        }
    }

    static <T0, T1> BiConsumer<T1, T0> swapConsumer(BiConsumer<T0, T1> act) {
        return (a, b) -> act.accept(b, a);
    }

    static <T0, T1, R> BiFunction<T1, T0, R> swapFunction(BiFunction<T0, T1, R> act) {
        return (a, b) -> act.apply(b, a);
    }

    @SuppressWarnings("rawtypes")
    Predicate TRUE_PREDICATE = x -> true;
    @SuppressWarnings("rawtypes")
    Predicate FALSE_PREDICATE = x -> false;

    @SuppressWarnings("unchecked")
    static <T> Predicate<T> truePredicate() {
        return ((Predicate<T>) TRUE_PREDICATE);
    }

    @SuppressWarnings("unchecked")
    static <T> Predicate<T> falsePredicate() {
        return ((Predicate<T>) FALSE_PREDICATE);
    }

}
