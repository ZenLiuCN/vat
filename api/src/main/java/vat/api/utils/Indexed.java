package vat.api.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

public record Indexed<T>(int index, T value) {
    public Indexed(AtomicInteger counter, T value) {
        this(counter.getAndIncrement(), value);
    }

    public <R> Indexed<R> map(Function<T, R> m) {
        return new Indexed<>(index, m.apply(value));
    }
    public  Indexed<T> mapIndex(IntUnaryOperator m) {
        return new Indexed<>(m.applyAsInt(index),value);
    }
}
