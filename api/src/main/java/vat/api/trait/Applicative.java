package vat.api.trait;


import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;
import vat.api.Data;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-11-19


public interface Applicative<T extends Applicative<T>> extends Data.Accessor<T> {
    T _this();

    default T accept(Consumer<T> consumer) {
        consumer.accept(_this());
        return _this();
    }

    default T accept(boolean cond, Consumer<T> consumer) {
        if (cond) consumer.accept(_this());
        return _this();
    }

    default <R> R apply(Function<T, @Nullable R> mapper) {
        return mapper.apply(_this());
    }

    @Override
    default boolean test(Predicate<T> m) {
        return m.test(_this());
    }

    default Future<T> acceptFuture(Function<T, Future<Void>> consumer) {
        return consumer.apply(_this()).map(_this());
    }

    default Future<T> acceptFuture(boolean cond, Function<T, Future<Void>> consumer) {
        if (cond)
            return consumer.apply(_this()).map(_this());
        return Future.succeededFuture(_this());
    }

    default <R> Future<R> applyFuture(Function<T, Future<R>> mapper) {
        return mapper.apply(_this());
    }

    @Override
    default Future<Boolean> testFuture(Function<T, Future<Boolean>> m) {
        return m.apply(_this());
    }
}
