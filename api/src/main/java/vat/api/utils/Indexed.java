package vat.api.utils;


import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

public record Indexed<T extends @Nullable Object>(int index, @Nullable T value) {
    public Indexed(AtomicInteger counter, @Nullable T value) {
        this(counter.getAndIncrement(), value);
    }

    public <R> Indexed<R> map(Function<@Nullable T, @Nullable R> m) {
        return new Indexed<>(index, m.apply(value));
    }

    public Indexed<T> mapIndex(IntUnaryOperator m) {
        return new Indexed<>(m.applyAsInt(index), value);
    }
}
