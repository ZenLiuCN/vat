package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

///
/// @author Zen.Liu
/// @since 2025-10-27


@SuppressWarnings("unused")
@NullMarked
public interface Buf {
    BiConsumer<Long, Buf> I64 = Fn.swapConsumer(Buf::i64);
    BiConsumer<Long, Buf> Z64 = Fn.swapConsumer(Buf::z64);
    BiConsumer<Long, Buf> V64 = Fn.swapConsumer(Buf::v64);
    BiConsumer<Integer, Buf> I32 = Fn.swapConsumer(Buf::i32);
    BiConsumer<Integer, Buf> Z32 = Fn.swapConsumer(Buf::z32);
    BiConsumer<Integer, Buf> V32 = Fn.swapConsumer(Buf::v32);

    /// internal buffer
    Buffer raw();

    default JsonArray toJsonArray() {
        return raw().toJsonArray();
    }

    default JsonObject toJsonObject() {
        return raw().toJsonObject();
    }

    default Buf append(Buf v) {
        raw().appendBuffer(v.raw());
        return this;
    }

    Buf writeBuffer(Buffer v);

    /// returns a copied buffer
    Buffer readBuffer(int size);

    Buf slice(int len);

    int size();

    int position();

    byte[] bytes(int n);

    Buf bytes(byte[] bytes);

    default boolean bool() {
        return i8() > 0;
    }

    default Buf bool(boolean b) {
        return i8((byte) (b ? 1 : 0));
    }

    byte i8();

    Buf i8(byte v);

    short i16();

    Buf i16(short v);

    int i32();

    Buf i32(int v);

    long i64();

    Buf i64(long v);

    float f32();

    Buf f32(float v);

    double f64();

    Buf f64(double v);

    short i16LE();

    Buf i16LE(short v);

    int i32LE();

    Buf i32LE(int v);

    long i64LE();

    Buf i64LE(long v);

    float f32LE();

    Buf f32LE(float v);

    double f64LE();

    Buf f64LE(double v);

    ///  variable integer
    default Buf v32(int n) {
        while (true) {
            if ((n & ~0x7F) == 0) {
                i8((byte) n);
                break;
            } else {
                i8(((byte) ((n & 0x7F) | 0x80)));
                n >>>= 7;
            }
        }
        return this;
    }

    ///  variable integer
    default int v32() {
        var result = 0;
        var shift = 0;
        while (true) {
            var by = i8();
            result |= (by & 0x7f) << shift;
            if ((by & 0x80) != 0x80)
                break;
            shift += 7;
        }
        return result;
    }

    ///  variable integer
    default Buf v64(long n) {
        while (true) {
            if ((n & ~0x7FL) == 0) {
                i8((byte) n);
                break;
            } else {
                i8(((byte) ((n & 0x7FL) | 0x80L)));
                n >>>= 7;
            }
        }
        return this;
    }

    ///  variable integer
    default long v64() {
        var result = 0L;
        var shift = 0L;
        while (true) {
            var by = i8();
            result |= (by & 0x7fL) << shift;
            if ((by & 0x80) != 0x80)
                break;
            shift += 7L;
        }
        return result;
    }

    ///  zigzag integer
    default Buf z32(int v) {
        v = (v << 1) ^ (v >> 31);
        return v32(v);
    }

    ///  zigzag integer
    default int z32() {
        var n = v32();
        n = (n >>> 1) ^ -(n & 1);
        return n;
    }

    ///  zigzag integer
    default Buf z64(long v) {
        v = (v << 1L) ^ (v >> 63L);
        return v64(v);
    }

    ///  zigzag integer
    default long z64() {
        var n = v64();
        n = (n >>> 1) ^ -(n & 1);
        return n;
    }

    default char character() {
        return (char) i32();
    }

    default Buf character(char v) {
        return i32(v);
    }

    default @Nullable String string() {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return "";
        var b = bytes(n);
        return new String(b, StandardCharsets.UTF_8);
    }

    default Buf string(@Nullable String v) {
        if (v == null) return z32(-1);
        if (v.isEmpty()) return z32(0);
        var b = v.getBytes(StandardCharsets.UTF_8);
        return bytes(b);
    }

    default <T> Buf object(@Nullable T obj, BiConsumer<T, Buf> write) {
        if (obj == null) return bool(false);
        bool(true);
        write.accept(obj, this);
        return this;
    }

    default <T> @Nullable T object(Function<Buf, @Nullable T> write) {
        var b = bool();
        if (!b) return null;
        return write.apply(this);
    }

    default byte @Nullable [] binary() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new byte[0];
        return bytes(v);
    }

    default Buf binary(byte @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        return z32(v.length).bytes(v);
    }

    default Buf buffer(@Nullable Buffer v) {
        if (v == null) return z32(-1);
        if (v.length() == 0) return z32(0);
        return z32(v.length()).writeBuffer(v);
    }

    default @Nullable Buffer buffer() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return Buffer.buffer();
        return readBuffer(v);
    }

    default <R> @Nullable R nullable(Function<Buf, R> read) {
        return bool() ? read.apply(this) : null;
    }

    default <R> Buf nullable(BiFunction<Buf, R, Buf> write, @Nullable R v) {
        if (v == null) {
            bool(false);
            return this;
        }
        return write.apply(bool(true), v);
    }

    default @Nullable Boolean booleanObject() {
        return nullable(Buf::bool);
    }

    default @Nullable Byte byteObject() {
        return nullable(Buf::i8);
    }

    default @Nullable Short shortObject() {
        return nullable(Buf::i16);
    }

    default @Nullable Integer integerObject() {
        return nullable(Buf::z32);
    }

    default @Nullable Long longObject() {
        return nullable(Buf::z64);
    }

    default @Nullable Float floatObject() {
        return nullable(Buf::f32);
    }

    default @Nullable Double doubleObject() {
        return nullable(Buf::f64);
    }

    default @Nullable Character characterObject() {
        return bool() ? (char) i32() : null;
    }

    default Buf booleanObject(@Nullable Boolean v) {
        return nullable(Buf::bool, v);
    }

    default Buf byteObject(@Nullable Byte v) {
        return nullable(Buf::i8, v);
    }

    default Buf shortObject(@Nullable Short v) {
        return nullable(Buf::i16, v);
    }

    default Buf integerObject(@Nullable Integer v) {
        return nullable(Buf::z32, v);
    }

    default Buf longObject(@Nullable Long v) {
        return nullable(Buf::z64, v);
    }

    default Buf floatObject(@Nullable Float v) {
        return nullable(Buf::f32, v);
    }

    default Buf doubleObject(@Nullable Double v) {
        return nullable(Buf::f64, v);
    }

    default Buf characterObject(@Nullable Character v) {
        return v == null ? bool(false) : bool(true).i32((int) v);
    }

    default boolean @Nullable [] boolArray() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new boolean[0];
        var x = new boolean[v];
        for (int i = 0; i < v; i++) {
            x[i] = bool();
        }
        return x;
    }

    default Buf boolArray(boolean @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (boolean b : v) {
            bool(b);
        }
        return this;
    }

    default short @Nullable [] i16Array() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new short[0];
        var x = new short[v];
        for (var i = 0; i < v; i++) {
            x[i] = i16();
        }
        return x;
    }

    default Buf i16Array(short @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            i16(b);
        }
        return this;
    }

    default int @Nullable [] i32Array() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new int[0];
        var x = new int[v];
        for (int i = 0; i < v; i++) {
            x[i] = i32();
        }
        return x;
    }

    default Buf i32Array(int @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            i32(b);
        }
        return this;
    }

    default long @Nullable [] i64Array() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new long[0];
        var x = new long[v];
        for (var i = 0; i < v; i++) {
            x[i] = i64();
        }
        return x;
    }

    default Buf i64Array(long @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            i64(b);
        }
        return this;
    }

    default float @Nullable [] f32Array() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new float[0];
        var x = new float[v];
        for (var i = 0; i < v; i++) {
            x[i] = f32();
        }
        return x;
    }

    default Buf f32Array(float @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            f32(b);
        }
        return this;
    }

    default double @Nullable [] f64Array() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new double[0];
        var x = new double[v];
        for (var i = 0; i < v; i++) {
            x[i] = f64();
        }
        return x;
    }

    default Buf f64Array(double @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            f64(b);
        }
        return this;
    }

    default char @Nullable [] characterArray() {
        var v = z32();
        if (v < 0) return null;
        if (v == 0) return new char[0];
        var x = new char[v];
        for (var i = 0; i < v; i++) {
            x[i] = character();
        }
        return x;
    }

    default Buf characterArray(char @Nullable [] v) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (var b : v) {
            character(b);
        }
        return this;
    }


    void reset();


    interface IndexWriter<T extends @Nullable Object> {
        void accept(Buf buf, int index, T v);
    }

    interface IndexReader<T extends @Nullable Object> {
        T apply(Buf buf, int index);
    }

    default <T, E extends Collection<@Nullable T>> Buf repeat(@Nullable E v, IndexWriter<@Nullable T> write) {
        if (v == null) return z32(-1);
        if (v.isEmpty()) return z32(0);
        z32(v.size());
        var i = 0;
        for (T t : v) {
            write.accept(this, i, t);
            i++;
        }
        return this;
    }

    default <T, E extends Collection<@Nullable T>> Buf repeat(@Nullable E v, BiConsumer<@Nullable T, Buf> write) {
        if (v == null) return z32(-1);
        if (v.isEmpty()) return z32(0);
        z32(v.size());
        for (T t : v) {
            write.accept(t, this);
        }
        return this;
    }

    default <T, E extends Collection<@Nullable T>> @Nullable E repeat(IntFunction<E> v,
                                                                      IndexReader<@Nullable T> reader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        var x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x.add(reader.apply(this, i));
        }
        return x;
    }

    default <T, E extends Collection<@Nullable T>> @Nullable E repeat(IntFunction<E> v,
                                                                      Function<Buf, @Nullable T> reader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        var x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x.add(reader.apply(this));
        }
        return x;
    }

    default <K, V, E extends Map<K, @Nullable V>> Buf map(@Nullable E v, IndexWriter<K> keyWrite,
                                                          IndexWriter<@Nullable V> valueWrite) {
        if (v == null) return z32(-1);
        if (v.isEmpty()) return z32(0);
        z32(v.size());
        var i = 0;
        for (var t : v.entrySet()) {
            keyWrite.accept(this, i, t.getKey());
            valueWrite.accept(this, i, t.getValue());
            i++;
        }
        return this;
    }

    default <K, V, E extends Map<K, @Nullable V>> Buf map(@Nullable E v, BiConsumer<Buf, K> keyWrite,
                                                          BiConsumer<@Nullable V, Buf> valueWrite) {
        if (v == null) return z32(-1);
        if (v.isEmpty()) return z32(0);
        z32(v.size());
        for (var t : v.entrySet()) {
            keyWrite.accept(this, t.getKey());
            valueWrite.accept(t.getValue(), this);
        }
        return this;
    }

    default <K, V, E extends Map<K, @Nullable V>> @Nullable E map(IntFunction<E> v, IndexReader<K> keyReader,
                                                                  IndexReader<@Nullable V> valueReader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        var x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x.put(keyReader.apply(this, i), valueReader.apply(this, i));
        }
        return x;
    }

    default <K, V, E extends Map<K, @Nullable V>> @Nullable E map(IntFunction<E> v, Function<Buf, K> keyReader,
                                                                  Function<Buf, @Nullable V> valueReader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        var x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x.put(keyReader.apply(this), valueReader.apply(this));
        }
        return x;
    }

    default <T> Buf array(T @Nullable [] v, IndexWriter<T> write) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        var i = 0;
        for (T t : v) {
            write.accept(this, i, t);
            i++;
        }
        return this;
    }

    default <T> Buf array(T @Nullable [] v, BiConsumer<Buf, T> write) {
        if (v == null) return z32(-1);
        if (v.length == 0) return z32(0);
        z32(v.length);
        for (T t : v) {
            write.accept(this, t);
        }
        return this;
    }

    default <T> @Nullable T @Nullable [] array(IntFunction<@Nullable T[]> v, IndexReader<@Nullable T> reader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        @Nullable T[] x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x[i] = (reader.apply(this, i));
        }
        return x;
    }

    default <T> @Nullable T @Nullable [] array(IntFunction<@Nullable T[]> v, Function<Buf, @Nullable T> reader) {
        var n = z32();
        if (n < 0) return null;
        if (n == 0) return v.apply(0);
        @Nullable T[] x = v.apply(n);
        for (int i = 0; i < n; i++) {
            x[i] = (reader.apply(this));
        }
        return x;
    }


    default Buf buf(@Nullable Buf v) {
        if (v == null) return z32(-1);
        var n = v.raw().length();
        if (n == 0) return z32(0);
        z32(n);
        return append(v);
    }

    default @Nullable Buf buf() {
        var n = z32();
        if (n < 0) return null;
        return slice(n);
    }

    default Buf apply(Consumer<Buf> act) {
        act.accept(this);
        return this;
    }

    record Buffered(Buffer raw, AtomicInteger pos) implements Buf {
        Buffered(Buffer buffer) {
            this(buffer, new AtomicInteger(0));
        }

        @Override
        public Buf slice(int len) {
            return new Buffered(raw.slice(pos.get(), pos.addAndGet(len)));
        }

        @Override
        public Buf writeBuffer(Buffer v) {
            raw.appendBuffer(v);
            pos.addAndGet(v.length());
            return this;
        }

        @Override
        public Buffer readBuffer(int size) {
            var s = pos.get();
            var e = pos.addAndGet(size);
            return raw.slice(s, e).copy();
        }

        @Override
        public void reset() {
            pos.set(0);
        }

        @Override
        public int size() {
            return raw.length();
        }

        @Override
        public int position() {
            return pos.get();
        }

        @Override
        public byte[] bytes(int n) {
            return raw.getBytes(pos.get(), pos.addAndGet(n));
        }

        @Override
        public Buf bytes(byte @Nullable [] bytes) {
            if (bytes == null) return z32(-1);
            z32(bytes.length);
            pos.addAndGet(bytes.length);
            raw.appendBytes(bytes);
            return this;
        }

        @Override
        public byte i8() {
            return raw.getByte(pos.getAndIncrement());
        }

        @Override
        public Buf i8(byte v) {
            raw.appendByte(v);
            pos.incrementAndGet();
            return this;
        }

        @Override
        public short i16() {
            return raw.getShort(pos.getAndAdd(2));
        }

        @Override
        public Buf i16(short v) {
            raw.appendShort(v);
            pos.getAndAdd(2);
            return this;
        }

        @Override
        public int i32() {
            return raw.getInt(pos.getAndAdd(4));
        }

        @Override
        public Buf i32(int v) {
            raw.appendInt(v);
            pos.getAndAdd(4);
            return this;
        }

        @Override
        public long i64() {
            return raw.getLong(pos.getAndAdd(8));
        }

        @Override
        public Buf i64(long v) {
            raw.appendLong(v);
            pos.getAndAdd(8);
            return this;
        }

        @Override
        public float f32() {
            return raw.getFloat(pos.getAndAdd(4));
        }

        @Override
        public Buf f32(float v) {
            raw.appendFloat(v);
            pos.getAndAdd(4);
            return this;
        }

        @Override
        public double f64() {
            return raw.getDouble(pos.getAndAdd(8));
        }

        @Override
        public Buf f64(double v) {
            raw.appendDouble(v);
            pos.getAndAdd(8);
            return this;
        }


        @Override
        public short i16LE() {
            return raw.getShortLE(pos.getAndAdd(2));
        }

        @Override
        public Buf i16LE(short v) {
            raw.appendShortLE(v);
            pos.getAndAdd(2);
            return this;
        }

        @Override
        public int i32LE() {
            return raw.getIntLE(pos.getAndAdd(4));
        }

        @Override
        public Buf i32LE(int v) {
            raw.appendIntLE(v);
            pos.getAndAdd(4);
            return this;
        }

        @Override
        public long i64LE() {
            return raw.getLongLE(pos.getAndAdd(8));
        }

        @Override
        public Buf i64LE(long v) {
            raw.appendLongLE(v);
            pos.getAndAdd(8);
            return this;
        }

        @Override
        public float f32LE() {
            return raw.getFloatLE(pos.getAndAdd(4));
        }

        @Override
        public Buf f32LE(float v) {
            raw.appendFloatLE(v);
            pos.getAndAdd(4);
            return this;
        }

        @Override
        public double f64LE() {
            return raw.getDoubleLE(pos.getAndAdd(8));
        }

        @Override
        public Buf f64LE(double v) {
            raw.appendDoubleLE(v);
            pos.getAndAdd(8);
            return this;
        }
    }

    static Buf of(int initialize) {
        return new Buffered(Buffer.buffer(initialize));
    }

    static Buf of(Buffer buffer) {
        return new Buffered(buffer);
    }

    static Buf of() {
        return new Buffered(Buffer.buffer());
    }
}
