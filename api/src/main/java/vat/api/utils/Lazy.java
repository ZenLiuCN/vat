package vat.api.utils;

import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;

import java.util.function.*;

///
/// @author Zen.Liu
/// @since 2025-11-03
@SuppressWarnings({"unused", "DuplicatedCode"})
public interface Lazy<T extends @Nullable Object> {
    T get();


    final class Delay<T> implements Lazy<T> {
        @Nullable
        public volatile T v;
        @Nullable
        public volatile Supplier<T> s;

        public Delay(Supplier<T> s) {
            this.s = s;
        }

        public T get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    assert x != null;
                    s = null;
                    return v = x.get();
                }
            }
            return v;
        }

    }

    final class DelayError<T> implements Lazy<T> {
        @Nullable
        public volatile T v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile Supplier<T> s;

        public DelayError(Supplier<T> s) {
            this.s = s;
        }

        @SneakyThrows
        public T get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    assert x != null;
                    s = null;
                    try {
                        v = x.get();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null)  //noinspection DataFlowIssue
                throw err;
            return v;
        }

    }

    interface Mutable<T extends @Nullable Object> extends Lazy<T> {
        void set(T v);

    }

    final class MutableDelay<T extends @Nullable Object> implements Mutable<T> {
        @Nullable
        public volatile T v;
        @Nullable
        public volatile Supplier<T> s;

        public MutableDelay(Supplier<T> s) {
            this.s = s;
        }

        public T get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    assert x != null;
                    s = null;
                    return v = x.get();
                }
            }
            return v;
        }

        @Override
        public void set(T v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    final class MutableDelayError<T extends @Nullable Object> implements Mutable<T> {
        @Nullable
        public volatile T v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile Supplier<T> s;

        public MutableDelayError(Supplier<T> s) {
            this.s = s;
        }

        @SneakyThrows
        public T get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.get();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }

        @Override
        public void set(T v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Delay<>(supplier);
    }

    /// capture and rethrows error
    static <T> Lazy<T> ofError(Supplier<T> supplier) {
        return new DelayError<>(supplier);
    }

    static <T> Mutable<T> mutable(Supplier<T> supplier) {
        return new MutableDelay<>(supplier);
    }

    /// capture and rethrows error
    static <T> Mutable<T> mutableError(Supplier<T> supplier) {
        return new MutableDelayError<>(supplier);
    }


    interface LazyBoolean {
        boolean get();
    }

    interface MutableBoolean extends LazyBoolean {
        void set(boolean v);
    }

    final class DelayBoolean implements LazyBoolean {
        public volatile boolean v;
        @Nullable
        public volatile BooleanSupplier s;

        public DelayBoolean(BooleanSupplier s) {
            this.s = s;
        }

        public boolean get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsBoolean();
                }
            }
            return v;
        }

    }

    final class DelayErrorBoolean implements LazyBoolean {
        public volatile boolean v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile BooleanSupplier s;

        public DelayErrorBoolean(BooleanSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public boolean get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsBoolean();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }
    }

    final class MutableDelayBoolean implements MutableBoolean {
        public volatile boolean v;
        @Nullable
        public volatile BooleanSupplier s;

        public MutableDelayBoolean(BooleanSupplier s) {
            this.s = s;
        }

        public boolean get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsBoolean();
                }
            }
            return v;
        }

        @Override
        public void set(boolean v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    final class MutableDelayErrorBoolean implements MutableBoolean {
        public volatile boolean v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile BooleanSupplier s;

        public MutableDelayErrorBoolean(BooleanSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public boolean get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsBoolean();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }

        @Override
        public void set(boolean v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    static LazyBoolean of(BooleanSupplier supplier) {
        return new DelayBoolean(supplier);
    }

    /// capture and rethrows error
    static LazyBoolean ofError(BooleanSupplier supplier) {
        return new DelayErrorBoolean(supplier);
    }

    static MutableBoolean mutable(BooleanSupplier supplier) {
        return new MutableDelayBoolean(supplier);
    }

    interface LazyInt {
        int get();
    }

    interface MutableInt extends LazyInt {
        void set(int v);
    }

    final class DelayInt implements LazyInt {
        public volatile int v;
        @Nullable
        public volatile IntSupplier s;

        public DelayInt(IntSupplier s) {
            this.s = s;
        }

        public int get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsInt();
                }
            }
            return v;
        }

    }

    final class DelayErrorInt implements LazyInt {
        public volatile int v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile IntSupplier s;

        public DelayErrorInt(IntSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public int get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsInt();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }
    }

    final class MutableDelayInt implements MutableInt {
        public volatile int v;
        @Nullable
        public volatile IntSupplier s;

        public MutableDelayInt(IntSupplier s) {
            this.s = s;
        }

        public int get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsInt();
                }
            }
            return v;
        }

        @Override
        public void set(int v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    final class MutableDelayErrorInt implements MutableInt {
        public volatile int v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile IntSupplier s;

        public MutableDelayErrorInt(IntSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public int get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsInt();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }

        @Override
        public void set(int v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    static LazyInt of(IntSupplier supplier) {
        return new DelayInt(supplier);
    }

    /// capture and rethrows error
    static LazyInt ofError(IntSupplier supplier) {
        return new DelayErrorInt(supplier);
    }

    static MutableInt mutable(IntSupplier supplier) {
        return new MutableDelayInt(supplier);
    }

    /// capture and rethrows error
    static MutableInt mutableError(IntSupplier supplier) {
        return new MutableDelayErrorInt(supplier);
    }

    interface LazyLong {
        long get();
    }

    interface MutableLong extends LazyLong {
        void set(long v);
    }

    final class DelayLong implements LazyLong {
        public volatile long v;
        @Nullable
        public volatile LongSupplier s;

        public DelayLong(LongSupplier s) {
            this.s = s;
        }

        public long get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsLong();
                }
            }
            return v;
        }

    }

    final class DelayErrorLong implements LazyLong {
        public volatile long v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile LongSupplier s;

        public DelayErrorLong(LongSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public long get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsLong();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }
    }

    final class MutableDelayLong implements MutableLong {
        public volatile long v;
        @Nullable
        public volatile LongSupplier s;

        public MutableDelayLong(LongSupplier s) {
            this.s = s;
        }

        public long get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsLong();
                }
            }
            return v;
        }

        @Override
        public void set(long v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    final class MutableDelayErrorLong implements MutableLong {
        public volatile long v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile LongSupplier s;

        public MutableDelayErrorLong(LongSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public long get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsLong();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }

        @Override
        public void set(long v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    static LazyLong of(LongSupplier supplier) {
        return new DelayLong(supplier);
    }

    /// capture and rethrows error
    static LazyLong ofError(LongSupplier supplier) {
        return new DelayErrorLong(supplier);
    }

    static MutableLong mutable(LongSupplier supplier) {
        return new MutableDelayLong(supplier);
    }

    /// capture and rethrows error
    static MutableLong mutableError(LongSupplier supplier) {
        return new MutableDelayErrorLong(supplier);
    }

    interface LazyDouble {
        double get();
    }

    interface MutableDouble extends LazyDouble {
        void set(double v);
    }

    final class DelayDouble implements LazyDouble {
        public volatile double v;
        @Nullable
        public volatile DoubleSupplier s;

        public DelayDouble(DoubleSupplier s) {
            this.s = s;
        }

        public double get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsDouble();
                }
            }
            return v;
        }

    }


    final class DelayErrorDouble implements LazyDouble {
        public volatile double v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile DoubleSupplier s;

        public DelayErrorDouble(DoubleSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public double get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsDouble();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }
    }

    final class MutableDelayDouble implements MutableDouble {
        public volatile double v;
        @Nullable
        public volatile DoubleSupplier s;

        public MutableDelayDouble(DoubleSupplier s) {
            this.s = s;
        }

        public double get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    assert x != null;
                    return v = x.getAsDouble();
                }
            }
            return v;
        }

        @Override
        public void set(double v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    final class MutableDelayErrorDouble implements MutableDouble {
        public volatile double v;
        @Nullable
        public volatile Exception err;
        @Nullable
        public volatile DoubleSupplier s;

        public MutableDelayErrorDouble(DoubleSupplier s) {
            this.s = s;
        }

        @SneakyThrows
        public double get() {
            if (s != null) {
                synchronized (this) {
                    if (s == null) return v;
                    var x = s;
                    s = null;
                    try {
                        assert x != null;
                        v = x.getAsDouble();
                    } catch (Exception e) {
                        err = e;
                        throw e;
                    }
                    return v;
                }
            }
            if (err != null) {
                //noinspection DataFlowIssue
                throw err;
            }
            return v;
        }

        @Override
        public void set(double v) {
            this.v = v;
            if (s != null) synchronized (this) {
                s = null;
            }
        }
    }

    static LazyDouble of(DoubleSupplier supplier) {
        return new DelayDouble(supplier);
    }

    /// capture and rethrows error
    static LazyDouble ofError(DoubleSupplier supplier) {
        return new DelayErrorDouble(supplier);
    }

    static MutableDouble mutable(DoubleSupplier supplier) {
        return new MutableDelayDouble(supplier);
    }

    /// capture and rethrows error
    static MutableDouble mutableError(DoubleSupplier supplier) {
        return new MutableDelayErrorDouble(supplier);
    }

}
