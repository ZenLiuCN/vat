package vat.api.utils;

import io.netty.util.collection.*;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface Primitives {
    interface ToShortFunction<T> extends Function<T, Short> {
        short applyAsShort(T v);

        @Override
        default Short apply(T t) {
            return applyAsShort(t);
        }
    }

    interface ToCharFunction<T> extends Function<T, Character> {
        char applyAsChar(T v);

        @Override
        default Character apply(T t) {
            return applyAsChar(t);
        }
    }

    interface ToByteFunction<T> extends Function<T, Byte> {
        byte applyAsByte(T v);

        @Override
        default Byte apply(T t) {
            return applyAsByte(t);
        }
    }

    static <T, U extends IntObjectMap<T>> BiFunction<U, ? super T, U> accumulator(ToIntFunction<T> classifier) {
        return (u, t) -> {
            u.put(classifier.applyAsInt(t), t);
            return u;
        };
    }

    static <T, U extends LongObjectMap<T>> BiFunction<U, ? super T, U> accumulator(ToLongFunction<T> classifier) {
        return (u, t) -> {
            u.put(classifier.applyAsLong(t), t);
            return u;
        };
    }

    static <T, U extends CharObjectMap<T>> BiFunction<U, ? super T, U> accumulator(ToCharFunction<T> classifier) {
        return (u, t) -> {
            u.put(classifier.applyAsChar(t), t);
            return u;
        };
    }

    static <T, U extends ByteObjectMap<T>> BiFunction<U, ? super T, U> accumulator(ToByteFunction<T> classifier) {
        return (u, t) -> {
            u.put(classifier.applyAsByte(t), t);
            return u;
        };
    }

    static <T, U extends ShortObjectMap<T>> BiFunction<U, ? super T, U> accumulator(ToShortFunction<T> classifier) {
        return (u, t) -> {
            u.put(classifier.applyAsShort(t), t);
            return u;
        };
    }

    static <T, M extends IntObjectMap<T>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }

    static <T, M extends LongObjectMap<T>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }

    static <T, M extends ShortObjectMap<T>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }

    static <T, M extends ByteObjectMap<T>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }

    static <T, M extends CharObjectMap<T>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }
}
