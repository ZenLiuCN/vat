package vat.api.implement;


import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.function.Function3;
import org.jspecify.annotations.Nullable;
import vat.api.Activities;
import vat.api.Data;
import vat.api.Domain;
import vat.api.DomainError;
import vat.api.utils.Buf;
import vat.api.utils.Cases;
import vat.api.utils.Fn;

import java.lang.reflect.Array;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Generated one for each domain.
 * Contains Type converters for objects in current domain.
 *
 * @author Zen.Liu
 * @since 2025-10-21
 */
@SuppressWarnings("unused")
public interface Codec<T> extends ReaderWriter.DataReader<T>, ReaderWriter.DataWriter<T>, ReaderWriter.Creator<T> {




    /// generated Codec implement Provider as final class without any instance methods. The implement must be named as `Codecs`.
    interface Provider {

    }

    interface InternalProvider {

    }
    //region JSON properties

    interface ObjectReader<T extends @Nullable Object> extends Function2<JsonObject, String, @Nullable T> {
        @Nullable T apply(JsonObject o, String key);

        default <R> ObjectReader<R> map(Function<T, R> act) {
            return (o, k) -> Optional.ofNullable(apply(o, k)).map(act).orElse(null);
        }

        ObjectReader<String> STRING = JsonObject::getString;
        ObjectReader<Integer> INTEGER = JsonObject::getInteger;
        ObjectReader<Number> NUMBER = JsonObject::getNumber;
        ObjectReader<Long> LONG = JsonObject::getLong;
        ObjectReader<Float> FLOAT = JsonObject::getFloat;
        ObjectReader<Double> DOUBLE = JsonObject::getDouble;
        ObjectReader<Boolean> BOOLEAN = JsonObject::getBoolean;
        ObjectReader<JsonObject> JSON_OBJECT = JsonObject::getJsonObject;
        ObjectReader<JsonArray> JSON_ARRAY = JsonObject::getJsonArray;
        ObjectReader<byte[]> BINARY = JsonObject::getBinary;
        ObjectReader<Buffer> BUFFER = JsonObject::getBuffer;
        ObjectReader<Instant> INSTANT = JsonObject::getInstant;

    }

    interface ObjectWriter<T extends @Nullable Object> extends Function3<JsonObject, String, @Nullable T, JsonObject> {
        JsonObject apply(JsonObject o, String key, @Nullable T v);

        default <R> ObjectWriter<R> map(Function<R, T> act) {
            return (o, k, v) -> apply(o, k, v == null ? null : act.apply(v));
        }

        ObjectWriter<String> STRING = JsonObject::put;
        ObjectWriter<Integer> INTEGER = JsonObject::put;
        ObjectWriter<Number> NUMBER = JsonObject::put;
        ObjectWriter<Long> LONG = JsonObject::put;
        ObjectWriter<Float> FLOAT = JsonObject::put;
        ObjectWriter<Double> DOUBLE = JsonObject::put;
        ObjectWriter<Boolean> BOOLEAN = JsonObject::put;
        ObjectWriter<JsonObject> JSON_OBJECT = JsonObject::put;
        ObjectWriter<JsonArray> JSON_ARRAY = JsonObject::put;
        ObjectWriter<byte[]> BINARY = JsonObject::put;
        ObjectWriter<Buffer> BUFFER = JsonObject::put;
        ObjectWriter<Instant> INSTANT = JsonObject::put;


    }

    interface ArrayReader<T extends @Nullable Object> extends Function2<JsonArray, Integer, @Nullable T> {
        @Nullable T apply(JsonArray o, int key);

        @Override
        default @Nullable T apply(JsonArray v1, Integer v2) {
            return apply(v1, v2.intValue());
        }

        default <R> ArrayReader<R> map(Function<T, R> act) {
            return (o, k) -> Optional.ofNullable(apply(o, k)).map(act).orElse(null);
        }

        ArrayReader<String> STRING = JsonArray::getString;
        ArrayReader<Integer> INTEGER = JsonArray::getInteger;
        ArrayReader<Number> NUMBER = JsonArray::getNumber;
        ArrayReader<Long> LONG = JsonArray::getLong;
        ArrayReader<Float> FLOAT = JsonArray::getFloat;
        ArrayReader<Double> DOUBLE = JsonArray::getDouble;
        ArrayReader<Boolean> BOOLEAN = JsonArray::getBoolean;
        ArrayReader<JsonObject> JSON_OBJECT = JsonArray::getJsonObject;
        ArrayReader<JsonArray> JSON_ARRAY = JsonArray::getJsonArray;
        ArrayReader<byte[]> BINARY = JsonArray::getBinary;
        ArrayReader<Buffer> BUFFER = JsonArray::getBuffer;
        ArrayReader<Instant> INSTANT = JsonArray::getInstant;


    }

    interface ArrayWriter<T extends @Nullable Object> extends Function3<JsonArray, Integer, @Nullable T, JsonArray> {
        JsonArray apply(JsonArray o, int key, @Nullable T v);

        @Override
        default JsonArray apply(JsonArray v1, Integer v2, @Nullable T v3) {
            return apply(v1, v2.intValue(), v3);
        }

        default <R> ArrayWriter<R> map(Function<R, T> act) {
            return (o, k, v) -> apply(o, k, v == null ? null : act.apply(v));
        }

        ArrayWriter<String> STRING = JsonArray::set;
        ArrayWriter<Integer> INTEGER = JsonArray::set;
        ArrayWriter<Number> NUMBER = JsonArray::set;
        ArrayWriter<Long> LONG = JsonArray::set;
        ArrayWriter<Float> FLOAT = JsonArray::set;
        ArrayWriter<Double> DOUBLE = JsonArray::set;
        ArrayWriter<Boolean> BOOLEAN = JsonArray::set;
        ArrayWriter<JsonObject> JSON_OBJECT = JsonArray::set;
        ArrayWriter<JsonArray> JSON_ARRAY = JsonArray::set;
        ArrayWriter<byte[]> BINARY = JsonArray::set;
        ArrayWriter<Buffer> BUFFER = JsonArray::set;
        ArrayWriter<Instant> INSTANT = JsonArray::set;

    }


    record MapReader<K, V, M extends Map<K, @Nullable V>>(
            IntFunction<M> ctor,
            ArrayReader<K> k,
            ArrayReader<V> v
    )
            implements ObjectReader<M> {
        @Override
        public @Nullable M apply(JsonObject o, String key) {
            var j = o.getJsonArray(key);
            if (j == null) return null;
            if (j.isEmpty()) return ctor.apply(0);
            var m = ctor.apply(j.size() / 2);
            for (int i = 0; i < j.size(); i += 2) {
                m.put(Objects.requireNonNull(k.apply(j, i)), (v.apply(j, i + 1)));
            }
            return m;
        }
    }

    record MapWriter<K, V, M extends Map<K, @Nullable V>>(
            ArrayWriter<K> k,
            ArrayWriter<V> v
    )
            implements ObjectWriter<M> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public JsonObject apply(JsonObject o, String key, @Nullable M m) {
            if (m == null) return o;
            if (m.isEmpty()) return o.put(key, new ArrayList<>(0));
            var j = new JsonArray(new ArrayList<>(m.size() * 2));
            var i = new AtomicInteger(0);
            m.forEach((ke, val) -> {
                j.addNull();
                k.apply(j, i.getAndIncrement(), ke);
                j.addNull();
                v.apply(j, i.getAndIncrement(), val);
            });
            return o;
        }
    }

    record RepeatReader<V, C extends Collection<@Nullable V>>(
            IntFunction<C> ctor,
            ArrayReader<V> v
    )
            implements ObjectReader<C> {
        @Override
        public @Nullable C apply(JsonObject o, String key) {
            var j = o.getJsonArray(key);
            if (j == null) return null;
            if (j.isEmpty()) return ctor.apply(0);
            var c = ctor.apply(j.size());
            for (int i = 0; i < j.size(); i++) {
                c.add(v.apply(j, i));
            }
            return c;
        }
    }

    record RepeatWriter<V, C extends Collection<@Nullable V>>(
            ArrayWriter<V> v
    )
            implements ObjectWriter<C> {
        @SuppressWarnings("DuplicatedCode")
        @Override
        public JsonObject apply(JsonObject o, String key, @Nullable C m) {
            if (m == null) return o;
            if (m.isEmpty()) return o.put(key, new ArrayList<>(0));
            var j = new JsonArray(new ArrayList<>(m.size() * 2));
            var i = new AtomicInteger(0);
            m.forEach((ke) -> {
                j.addNull();
                if (ke != null)
                    v.apply(j, i.getAndIncrement(), ke);
            });
            return o;
        }
    }

    record ArrayTypeWriter<T>(ArrayWriter<T> v) implements ObjectWriter<T[]> {

        @Override
        public JsonObject apply(JsonObject o, String key, @Nullable T @Nullable [] m) {
            if (m == null) return o;
            if (m.length == 0) return o.put(key, new ArrayList<>(0));
            var j = new JsonArray(new ArrayList<>(m.length));
            for (int i = 0; i < m.length; i++) {
                j.addNull();
                if (m[i] != null)
                    v.apply(j, i, m[i]);
            }
            return o;
        }
    }

    record ArrayTypeReader<T>(Class<T> type,
                              ArrayReader<T> v) implements ObjectReader<T[]> {
        @SuppressWarnings("unchecked")
        @Override
        public @Nullable T @Nullable [] apply(JsonObject o, String key) {
            var j = o.getJsonArray(key);
            if (j == null) return null;
            if (j.isEmpty()) return (T[]) Array.newInstance(type, 0);
            @Nullable T[] x = (@Nullable T[]) Array.newInstance(type, j.size());
            for (var i = 0; i < j.size(); i++) {
                x[i] = v.apply(j, i);
            }
            return x;
        }
    }

    interface DataProperty<T extends @Nullable Object> {
        @Nullable T get(JsonObject o, String key);

        /// preferred key getter, first none null key assign value as real value. Check out {@link vat.api.meta.Alias}
        default @Nullable T get(JsonObject o, String... key) {
            for (var s : key) {
                if (o.containsKey(s)) {
                    return get(o, s);
                }
            }
            return null;
        }

        JsonObject set(JsonObject o, String key, @Nullable T t);

    }

    record CombineArrayProperty<T>(ArrayTypeReader<T> r,
                                   ArrayTypeWriter<T> w) implements DataProperty<T[]> {
        public @Nullable T @Nullable [] get(JsonObject o, String key) {
            return r.apply(o, key);
        }

        public JsonObject set(JsonObject o, String key, @Nullable T @Nullable [] t) {
            return w.apply(o, key, t);
        }

        public <R> CombineProperty<R> map(Function<@Nullable T @Nullable [], @Nullable R> to, Function<@Nullable R, @Nullable T @Nullable []> from) {
            return new CombineProperty<>(r.map(to), w.map(from));
        }

    }

    record CombineProperty<T extends @Nullable Object>(ObjectReader<T> r,
                                                       ObjectWriter<T> w) implements DataProperty<T> {
        public @Nullable T get(JsonObject o, String key) {
            return r.apply(o, key);
        }

        public JsonObject set(JsonObject o, String key, @Nullable T t) {
            return w.apply(o, key, t);
        }

        public <R> CombineProperty<R> map(Function<@Nullable T, @Nullable R> to, Function<@Nullable R, @Nullable T> from) {
            return new CombineProperty<>(r.map(to), w.map(from));
        }

    }

    CombineProperty<String> STRING = new CombineProperty<>(ObjectReader.STRING, ObjectWriter.STRING);
    CombineProperty<Integer> INT = new CombineProperty<>(ObjectReader.INTEGER, ObjectWriter.INTEGER);
    CombineProperty<Integer> INTEGER = INT;
    CombineProperty<Number> NUMBER = new CombineProperty<>(ObjectReader.NUMBER, ObjectWriter.NUMBER);
    CombineProperty<Long> LONG = new CombineProperty<>(ObjectReader.LONG, ObjectWriter.LONG);
    CombineProperty<Float> FLOAT = new CombineProperty<>(ObjectReader.FLOAT, ObjectWriter.FLOAT);
    CombineProperty<Double> DOUBLE = new CombineProperty<>(ObjectReader.DOUBLE, ObjectWriter.DOUBLE);
    CombineProperty<Boolean> BOOLEAN = new CombineProperty<>(ObjectReader.BOOLEAN, ObjectWriter.BOOLEAN);
    CombineProperty<JsonObject> JSON_OBJECT = new CombineProperty<>(ObjectReader.JSON_OBJECT, ObjectWriter.JSON_OBJECT);
    CombineProperty<JsonArray> JSON_ARRAY = new CombineProperty<>(ObjectReader.JSON_ARRAY, ObjectWriter.JSON_ARRAY);
    CombineProperty<byte[]> BINARY = new CombineProperty<>(ObjectReader.BINARY, ObjectWriter.BINARY);
    CombineProperty<Buffer> BUFFER = new CombineProperty<>(ObjectReader.BUFFER, ObjectWriter.BUFFER);
    CombineProperty<Instant> INSTANT = new CombineProperty<>(ObjectReader.INSTANT, ObjectWriter.INSTANT);
    CombineProperty<Character> CHAR = NUMBER.map(Fn.nullable(i -> ((char) (i.intValue()))), Fn.nullable(c -> (int) c));
    CombineProperty<Byte> BYTE = NUMBER.map(Fn.nullable(Number::byteValue), Fn.nullable(c -> c));
    CombineProperty<Short> SHORT = NUMBER.map(Fn.nullable(Number::shortValue), Fn.nullable(c -> c));
    CombineProperty<Short> SHORT_OBJECT = SHORT;
    CombineProperty<Byte> BYTE_OBJECT = BYTE;
    CombineProperty<Integer> INT_OBJECT = INT;
    CombineProperty<Long> LONG_OBJECT = LONG;
    CombineProperty<Float> FLOAT_OBJECT = FLOAT;
    CombineProperty<Double> DOUBLE_OBJECT = DOUBLE;
    CombineProperty<Boolean> BOOLEAN_OBJECT = BOOLEAN;
    CombineProperty<Character> CHAR_OBJECT = CHAR;

    //endregion


    default ObjectReader<T> asObjectReader() {
        return (o, k) -> get(o.getJsonObject(k));
    }

    default ObjectWriter<T> asObjectWriter() {
        return (o, v, t) -> o.put(v, set(t));
    }

    interface JsDecoder<O extends Data> extends Function<JsonObject, O> {
        O apply(JsonObject j, @Nullable Void ignore);

        @Override
        default O apply(JsonObject value) {
            return apply(value, null);
        }
    }

    interface DataCodec<O extends T, T extends Data> extends Codec<T> {
        @Override
        default @Nullable T fromJson(@Nullable JsonObject json) {
            return get(json);
        }


        default @Nullable O buf(@Nullable Buf i) {
            return i == null ? null : get(i.toJsonObject());
        }

        default @Nullable Buf buf(@Nullable T i) {
            return i == null ? null : Buf.of(i.toJson().toBuffer());
        }

        @Nullable O get(@Nullable JsonObject o);

        @Nullable O from(@Nullable T t);

        @Override
        default @Nullable JsonObject set(@Nullable T t) {
            if (t == null) {
                return null;
            }
            return t.asJson();
        }

        static <O extends T, T extends Data> Closure<O, T> closure(
                Function<JsonObject, O> serialize,
                Class<O> impl) {
            return new Closure<>(serialize, impl);
        }

        record Closure<O extends T, T extends Data>(
                Function<JsonObject, O> serialize,
                Class<O> impl
        ) implements DataCodec<O, T> {

            @Override
            public @Nullable O get(@Nullable JsonObject o) {
                if (o == null) return null;
                return serialize.apply(o);
            }

            @Override
            public O from(T t) {
                return t == null ? null : impl.isInstance(t) ? impl.cast(t) : get(t.asJson());
            }
        }
    }

    interface BinaryCodec<O extends Data.Binary> extends BinaryProperty<O> {
        @Override
        default @Nullable O read(Buf buf) {
            return buf.bool() ? ctor(buf) : null;
        }

        O ctor(Buf buf);

        @Override
        default Buf write(Buf buf, @Nullable O v) {
            return v == null ? buf.bool(false) : buf.bool(true).apply(v::toBuf);
        }

        default Buf buf(@Nullable O v) {
            return write(Buf.of(), v);
        }

    }

    interface BinaryProperty<P extends @Nullable Object> {
        @Nullable P read(Buf buf);

        Buf write(Buf buf, @Nullable P v);

        default @Nullable P read(Buffer buf, int offset) {
            return read(Buf.of(buf.slice(offset, buf.length())));
        }

        default Buffer write(Buffer buf, int offset, @Nullable P v) {
            write(Buf.of(buf.slice(offset, buf.length())), v);
            return buf;
        }


        static <P> BinaryProperty.I<P> nullable(Function<Buf, @Nullable P> r, BiFunction<Buf, @Nullable P, Buf> w) {
            return new I<>(Fn.nullable(r), Fn.nullable(w));
        }

        static <P> BinaryProperty.I<P> nonNull(Function<Buf, @Nullable P> r, BiFunction<Buf, P, Buf> w) {
            return new I<>(Fn.nonNull(r), Fn.nonNull(w));
        }

        record I<P extends @Nullable Object>(Function<Buf, @Nullable P> r,
                                             BiFunction<Buf, @Nullable P, Buf> w) implements BinaryProperty<P> {

            @Override
            public @Nullable P read(Buf buf) {
                return r.apply(buf);
            }

            @Override
            public Buf write(Buf buf, @Nullable P v) {
                return w.apply(buf, v);
            }


            public <T> I<T> map(Function<T, P> w, Function<P, T> r) {
                var wx = this.w;
                var nw = Fn.nullable(w);
                return new I<>(this.r.andThen(Fn.nullable(r)), (b, t) -> wx.apply(b, nw.apply(t)));
            }

        }
    }

    Function<String, String> PASCAL_UPPER_SNAKE = Cases.convert(Cases.PASCAL_CASE, Cases.UPPER_SNAKE_CASE);
    Function<String, String> QUALIFIED_UPPER_SNAKE = Cases.convert(Cases.LOWER_QUALIFIED_CASE, Cases.UPPER_SNAKE_CASE);

    @SuppressWarnings("unchecked")
    static <T extends Data> Codec<T> codec(Class<T> type) {
        var codec = load(type);
        var name = PASCAL_UPPER_SNAKE.apply(type.getSimpleName());
        if (!name.endsWith("_DATA") && !name.endsWith("_OBJECT")) name += "_DATA";
        try {
            return (Codec<T>) codec.getField(name).get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Codec not found for " + type, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Data> Optional<JsDecoder<? extends T>> js(Class<T> type) {
        var codec = load(type);
        var name = PASCAL_UPPER_SNAKE.apply(type.getSimpleName());
        if (name.endsWith("_DATA") || name.endsWith("_OBJECT")) name += "_JS";
        else name += "_DATA_JS";
        try {
            return Optional.of((JsDecoder<? extends T>) codec.getField(name).get(null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Data> Optional<BinaryCodec<? extends T>> binary(Class<T> type) {
        var codec = load(type);
        var name = PASCAL_UPPER_SNAKE.apply(type.getSimpleName());
        if (name.endsWith("_DATA")) name += "_BINARY";
        else if (name.endsWith("_OBJECT")) name = name.substring(0, name.lastIndexOf("_OBJECT")) + "_DATA_BINARY";
        else name += "_DATA_BINARY";
        try {
            return Optional.of((BinaryCodec<? extends T>) codec.getField(name).get(null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Data, P extends Data.DataObject<? super T> & Data> Optional<Codec<P>> object(Class<T> type) {
        var codec = load(type);
        var name = PASCAL_UPPER_SNAKE.apply(type.getSimpleName());
        if (name.endsWith("_DATA")) name = name.substring(0, name.lastIndexOf("_DATA")) + "_OBJECT";
        else if (!name.endsWith("_OBJECT")) name += "_OBJECT";
        try {
            return Optional.ofNullable((Codec<P>) codec.getField(name).get(null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    interface ActivityFactory<T extends Activities> {
        T apply(Vertx vertx, @Nullable String address, @Nullable DeliveryOptions options);
    }


    @SuppressWarnings("unchecked")
    static <T extends Activities> ActivityFactory<T> activity(Class<T> type) {
        var codec = load(type);
        var name = PASCAL_UPPER_SNAKE.apply(type.getSimpleName());
        if (name.endsWith("_DOMAIN")) name = name.substring(0, name.lastIndexOf("_DOMAIN")) + "_ACTIVITIES";
        else if (name.endsWith("_PROXY")) name = name.substring(0, name.lastIndexOf("_PROXY")) + "_ACTIVITIES";
        else name = name + "_ACTIVITIES";
        try {
            return (ActivityFactory<T>) codec.getField(name).get(null);
        } catch (Exception e) {
            throw new IllegalStateException("ActivityFactory not found for " + type, e);
        }
    }

    static <T extends Domain> Class<?> load(Class<T> type) {
        try {
            var domainCodec = type.getPackageName() + ".Codecs";
            return type.getClassLoader().loadClass(domainCodec);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Codec container class not found: " + type, e);
        }
    }

    //region utilities
    static <T> T prim(@Nullable T v, String name) {
        return Objects.requireNonNull(v, () -> "Corrupt primitive value of " + name);
    }

    static @Nullable Object jsonAny(@Nullable Object v) {
        if (v == null) return null;
        else if (v instanceof Number ||
                 v instanceof Boolean ||
                 v instanceof String ||
                 v instanceof JsonArray ||
                 v instanceof byte[] ||
                 v instanceof Buffer ||
                 v instanceof Instant ||
                 v instanceof JsonObject
        ) return v;
        else if (v instanceof Data cs) return cs.asJson();
        else if (v instanceof CharSequence cs) return cs.toString();
        else if (v instanceof Collection<?> cs) {
            if (cs.isEmpty())
                return cs;
            var f = cs.iterator().next();
            if (jsonAny(f) != null) return cs;
        } else if (v instanceof Map<?, ?> cs) {
            if (!cs.isEmpty()) {
                var k = cs.keySet().iterator().next();
                var value = cs.get(k);
                if (k instanceof String && jsonAny(value) != null)
                    return cs;
            } else {
                return cs;
            }

        }
        throw DomainError.System.conflict("invalid json value of {}", v.getClass());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> Class<T> @Nullable [] clazz(String @Nullable [] fqn) {
        return fqn == null ? null : (Class<T>[]) Arrays.stream(fqn).map(Codec::clazz).toArray(Class[]::new);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> @Nullable Class<T> clazz(@Nullable String fqn) {
        return fqn == null ? null : (Class<T>) Class.forName(fqn, true, Codec.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> T instance(Class<?> clz) {
        return (T) clz.getConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> T instance(Class<?> clz, List<Map.Entry<Class<?>, Object>> parameters) {
        var type = parameters.stream().map(Map.Entry::getKey).toArray(Class<?>[]::new);
        var values = parameters.stream().map(Map.Entry::getValue).toArray();
        return (T) clz.getConstructor(type).newInstance(values);
    }

    static <T> @Nullable String clazz(@Nullable Class<T> fqn) {
        return fqn == null ? null : fqn.getCanonicalName();
    }

    @SneakyThrows
    static <T extends Data> @Nullable T data(@Nullable JsonObject v, @Nullable Class<T> type) {
        if (v == null) return null;
        if (type == null) type = clazz(Objects.requireNonNull(v.getString("$type")));
        return type.getConstructor(JsonObject.class).newInstance(v);
    }

    static @Nullable String charArray(char @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return "";
        return new String(v);
    }

    static char @Nullable [] charArray(@Nullable String v) {
        if (v == null) return null;
        if (v.isEmpty()) return new char[0];
        return v.toCharArray();
    }

    static byte @Nullable [] booleanArray(boolean @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new byte[0];
        int byteCount = (v.length + 7) / 8;
        byte[] bytes = new byte[byteCount + 4];  // Extra 4 bytes for length
        // Store length as 4-byte big-endian integer
        bytes[0] = (byte) ((v.length >> 24) & 0xFF);
        bytes[1] = (byte) ((v.length >> 16) & 0xFF);
        bytes[2] = (byte) ((v.length >> 8) & 0xFF);
        bytes[3] = (byte) (v.length & 0xFF);
        // Compress the boolean array starting at byte[4]
        for (int i = 0; i < v.length; i++) {
            if (v[i]) {
                int byteIndex = (i / 8) + 4;  // Start after length bytes
                int bitIndex = i % 8;
                bytes[byteIndex] |= (byte) (1 << bitIndex);
            }
        }
        return bytes;
    }

    static boolean @Nullable [] booleanArray(byte @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new boolean[0];
        int length = ((v[0] & 0xFF) << 24) |
                     ((v[1] & 0xFF) << 16) |
                     ((v[2] & 0xFF) << 8) |
                     (v[3] & 0xFF);

        var o = new boolean[length];
        // Decompress starting from byte[4]
        for (int i = 0; i < length; i++) {
            int byteIndex = (i / 8) + 4;
            int bitIndex = i % 8;
            o[i] = ((v[byteIndex] >> bitIndex) & 1) == 1;
        }
        return o;
    }

    static @Nullable JsonArray shortArray(short @Nullable [] shorts) {
        if (shorts == null) return null;
        if (shorts.length == 0) return new JsonArray();
        // Calculate required int array size (2 shorts per int + 1 for length)
        int intCount = (shorts.length + 1) / 2 + 1;
        var ints = new JsonArray(new ArrayList<>(intCount + 1));
        ints.add(shorts.length);
        var x = 0;
        // Compress data starting from second int
        for (int i = 0; i < shorts.length; i++) {
            if (i % 2 == 0) {
                // First short of pair - store in upper 16 bits
                x = shorts[i] << 16;
            } else {
                // Second short of pair - store in lower 16 bits
                x |= shorts[i] & 0xFFFF;
                ints.add(x);
                x = 0;
            }
        }
        if (x != 0) ints.add(x);
        return ints;
    }

    static short @Nullable [] shortArray(@Nullable JsonArray ints) {
        if (ints == null) return null;
        if (ints.isEmpty()) return new short[0];
        var length = ints.getInteger(0);
        short[] shorts = new short[length];

        // Decompress data starting from second int
        for (int i = 0; i < length; i++) {
            int sourceIndex = (i / 2) + 1;
            int v = ints.getInteger(i);
            if (i % 2 == 0) {
                // Get from upper 16 bits
                shorts[i] = (short) (v >>> 16);
            } else {
                // Get from lower 16 bits
                shorts[i] = (short) (v & 0xFFFF);
            }
        }
        return shorts;
    }

    static @Nullable JsonArray intArray(int @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new JsonArray();
        var o = new JsonArray(new ArrayList<>(v.length));
        for (var j : v) {
            o.add(j);
        }
        return o;
    }

    static int @Nullable [] intArray(@Nullable JsonArray v) {
        if (v == null) return null;
        if (v.isEmpty()) return new int[0];
        var o = new int[v.size()];
        for (var i = 0; i < v.size(); i++) {
            o[i] = v.getInteger(i);
        }
        return o;
    }


    static @Nullable JsonArray longArray(long @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new JsonArray();
        var o = new JsonArray(new ArrayList<>(v.length));
        for (var j : v) {
            o.add(j);
        }
        return o;
    }

    static long @Nullable [] longArray(@Nullable JsonArray v) {
        if (v == null) return null;
        if (v.isEmpty()) return new long[0];
        var o = new long[v.size()];
        for (var i = 0; i < v.size(); i++) {
            o[i] = v.getLong(i);
        }
        return o;
    }

    static @Nullable JsonArray floatArray(float @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new JsonArray();
        var o = new JsonArray(new ArrayList<>(v.length));
        for (var j : v) {
            o.add(j);
        }
        return o;
    }

    static float @Nullable [] floatArray(@Nullable JsonArray v) {
        if (v == null) return null;
        if (v.isEmpty()) return new float[0];
        var o = new float[v.size()];
        for (var i = 0; i < v.size(); i++) {
            o[i] = v.getFloat(i);
        }
        return o;
    }


    static @Nullable JsonArray doubleArray(double @Nullable [] v) {
        if (v == null) return null;
        if (v.length == 0) return new JsonArray();
        var o = new JsonArray(new ArrayList<>(v.length));
        for (var j : v) {
            o.add(j);
        }
        return o;
    }

    static double @Nullable [] doubleArray(@Nullable JsonArray v) {
        if (v == null) return null;
        if (v.isEmpty()) return new double[0];
        var o = new double[v.size()];
        for (var i = 0; i < v.size(); i++) {
            o[i] = v.getDouble(i);
        }
        return o;
    }

    interface JsonArrayWriter<T extends @Nullable Object> {
        void accept(int index, JsonArray ar, @Nullable T value);
    }

    interface JsonObjectWriter<T extends @Nullable Object> {
        void accept(String key, JsonObject ar, @Nullable T value);
    }

    interface JsonArrayReader<T extends @Nullable Object> {
        @Nullable T apply(int index, JsonArray ar);
    }

    interface JsonObjectReader<T extends @Nullable Object> {
        @Nullable T apply(String key, JsonObject ar);
    }

    static <T> @Nullable JsonArray generic(@Nullable T @Nullable [] v, JsonArrayWriter<T> act) {
        if (v == null) return null;
        var a = new JsonArray(new ArrayList<>(v.length));
        for (int i = 0; i < v.length; i++) {
            a.addNull();
            act.accept(i, a, v[i]);
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    static <T> @Nullable T @Nullable [] generic(@Nullable JsonArray v, Class<T> type, JsonArrayReader<T> act) {
        if (v == null) return null;
        var a = (T[]) Array.newInstance(type, v.size());
        if (v.isEmpty()) return a;
        for (int i = 0; i < v.size(); i++) {
            Array.set(a, i, act.apply(i, v));
        }
        return a;
    }

    static <T, R extends Collection<@Nullable T>> @Nullable R list(@Nullable JsonArray v, Supplier<R> ctor, JsonArrayReader<T> reader) {
        if (v == null) return null;
        if (v.isEmpty()) return ctor.get();
        var r = ctor.get();
        for (int i = 0; i < v.size(); i++) {
            r.add(reader.apply(i, v));
        }
        return r;
    }

    static <T, R extends Collection<T>> @Nullable JsonArray list(@Nullable R v, JsonArrayWriter<T> act) {
        if (v == null) return null;
        if (v.isEmpty()) return JsonArray.of();
        var r = new JsonArray(new ArrayList<>(v.size()));
        var it = v.iterator();
        for (var i = 0; i < v.size(); i++) {
            r.addNull();
            act.accept(i, r, it.next());
        }
        return r;
    }

    /// read format `[[K,V],[K,V]...]`
    static <K, V, M extends Map<K, @Nullable V>> @Nullable M map(@Nullable JsonArray v, Supplier<M> ctor, JsonArrayReader<K> kAct, JsonArrayReader<V> vAct) {
        if (v == null) return null;
        if (v.isEmpty()) return ctor.get();
        var r = ctor.get();
        for (int i = 0; i < v.size(); i++) {
            var j = v.getJsonArray(i);
            r.put(Objects.requireNonNull(kAct.apply(0, j)), j.size() < 2 ? null : vAct.apply(1, j));
        }
        return r;
    }

    /// formated as   `\[\[K,V\]\]`
    static <K, V, M extends Map<K, @Nullable V>> @Nullable JsonArray map(@Nullable M v, JsonArrayWriter<K> kAct, JsonArrayWriter<V> vAct) {
        if (v == null) return null;
        if (v.isEmpty()) return JsonArray.of();
        var r = new JsonArray(new ArrayList<>(v.size()));
        for (var e : v.entrySet()) {
            var j = new JsonArray(new ArrayList<>(2));
            var k = e.getKey();
            V vl = e.getValue();
            kAct.accept(0, j.addNull(), k);
            vAct.accept(1, j.addNull(), vl);
        }
        return r;
    }

    static <V, O> @Nullable V object(@Nullable O o, Function<O, V> act) {
        if (o == null) return null;
        return act.apply(o);
    }

    private static String units(String s) {
        var i = s.length() - 1;
        while (i >= 0) {
            var c = s.charAt(i);
            if (!Character.isLetter(c))
                break;
            i -= 1;
        }
        return s.substring(i + 1);
    }

    static boolean any(@Nullable String value, String... values) {
        if (value == null) return false;
        for (var s : values) {
            if (s.equals(value)) return true;
        }
        return false;
    }

    static @Nullable Duration duration(@Nullable String v) {
        if (v == null || v.isBlank()) return null;
        if (v.equals("0s")) return Duration.ZERO;
        v = v.strip();
        var srcUnit = units(v);
        var unit = srcUnit;
        var numberString = (v.substring(0, v.length() - srcUnit.length())).strip();
        TimeUnit units;
        if (numberString.isEmpty()) {
            throw new IllegalArgumentException("Missing number in duration value '" + v + "'");
        } else {
            if (srcUnit.length() > 2 && !srcUnit.endsWith("s")) {
                unit = srcUnit + "s";
            }
            if (any(unit, "ms", "millis", "milliseconds", "")) {
                units = TimeUnit.MILLISECONDS;
            } else if (any(unit, "us", "micros", "microseconds")) {
                units = TimeUnit.MICROSECONDS;
            } else if (any(unit, "ns", "nanos", "nanoseconds")) {
                units = TimeUnit.NANOSECONDS;
            } else if (any(unit, "d", "days")) {
                units = TimeUnit.DAYS;
            } else if (any(unit, "h", "hours")) {
                units = TimeUnit.HOURS;
            } else if (any(unit, "s", "seconds")) {
                units = TimeUnit.SECONDS;
            } else if (any(unit, "m", "minutes")) {
                units = TimeUnit.MINUTES;
            } else {
                throw new IllegalArgumentException(
                        "Could not parse time unit '" + srcUnit + "' only supports ns, us, ms, s, m, h, d");
            }
            try {
                if (numberString.matches("[+-]?[0-9]+")) {
                    return Duration.ofNanos(units.toNanos(Long.parseLong(numberString)));
                } else {
                    long nanosInUnit = units.toNanos(1L);
                    return Duration.ofNanos((long) (Double.parseDouble(numberString) * (double) nanosInUnit));
                }
            } catch (NumberFormatException ignore) {
                throw new IllegalArgumentException("Could not parse duration number '" + numberString + "'");
            }
        }
    }

    static @Nullable String duration(@Nullable Duration v) {
        if (v == null) return null;
        if (v.isZero()) return "0s";
        long nanos = v.toNanos();
        // Try to find the most appropriate unit
        if (nanos % TimeUnit.DAYS.toNanos(1) == 0) {
            long days = TimeUnit.NANOSECONDS.toDays(nanos);
            return days + "d";
        } else if (nanos % TimeUnit.HOURS.toNanos(1) == 0) {
            long hours = TimeUnit.NANOSECONDS.toHours(nanos);
            return hours + "h";
        } else if (nanos % TimeUnit.MINUTES.toNanos(1) == 0) {
            long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
            return minutes + "m";
        } else if (nanos % TimeUnit.SECONDS.toNanos(1) == 0) {
            long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
            return seconds + "s";
        } else if (nanos % TimeUnit.MILLISECONDS.toNanos(1) == 0) {
            long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
            return millis + "ms";
        } else if (nanos % TimeUnit.MICROSECONDS.toNanos(1) == 0) {
            long micros = TimeUnit.NANOSECONDS.toMicros(nanos);
            return micros + "us";
        } else {
            return nanos + "ns";
        }
    }

    static @Nullable Period period(@Nullable String v) {
        if (v == null || v.isBlank()) return null;
        v = v.strip();
        var srcUnit = units(v);
        var unit = srcUnit;
        var numbers = (v.substring(0, v.length() - srcUnit.length())).strip();
        ChronoUnit units;
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Missing number in period value '" + v + "'");
        } else {
            if (unit.length() > 2 && !unit.endsWith("s")) {
                unit = unit + "s";
            }
            if (any(unit, "", "d", "days")) {
                units = ChronoUnit.DAYS;
            } else if (any(unit, "w", "weeks")) {
                units = ChronoUnit.WEEKS;
            } else if (any(unit, "m", "mo", "months")) {
                units = ChronoUnit.MONTHS;
            } else if (any(unit, "y", "years")) {
                units = ChronoUnit.YEARS;
            } else {
                throw new IllegalArgumentException(
                        "Could not parse time unit '" + srcUnit + "' , only supports d, w, mo, y");
            }
            try {
                var num = Integer.parseInt(numbers);
                if (units.isTimeBased()) {
                    throw new DateTimeException(unit + " cannot be converted to a java.time.Period");
                }
                return switch (units) {
                    case DAYS -> Period.ofDays(num);
                    case WEEKS -> Period.ofWeeks(num);
                    case MONTHS -> Period.ofMonths(num);
                    case YEARS -> Period.ofYears(num);
                    default -> throw new DateTimeException(unit + " cannot be converted to a java.time.Period");
                };
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse period number '" + numbers + "'");
            }
        }

    }

    static @Nullable String period(@Nullable Period v) {
        if (v == null) return null;
        if (v.isZero()) return "0d";
        var y = v.getYears();
        var m = v.getMonths();
        var d = v.getDays();
        if (y != 0 && m == 0 && d == 0) return y + "y";
        if (y == 0 && m != 0 && d == 0) return m + "m";
        if (y == 0 && m == 0 && d != 0) return d % 7 == 0 ? (d / 7 + "w") : (d + "d");
        long totalDays = v.getYears() * 365L + v.getMonths() * 30L + v.getDays();
        return totalDays + "d";
    }

    static <T extends Enum<T>> @Nullable Integer enumerate(@Nullable T v) {
        if (v == null) return null;
        return v.ordinal();
    }

    static <T extends Enum<T>> @Nullable T enumerate(@Nullable Integer v, Class<T> c) {
        if (v == null) return null;
        var x = c.getEnumConstants();
        var i = (int) v;
        if (i < 0 || i >= x.length) return null;
        return x[i];
    }

    static <T extends Enum<T>> @Nullable String enumerateText(@Nullable T v) {
        if (v == null) return null;
        return v.name();
    }

    static <T extends Enum<T>> @Nullable T enumerateText(@Nullable String v, Class<T> c) {
        if (v == null || v.isBlank()) return null;
        try {
            return Enum.valueOf(c, v);
        } catch (Exception e) {
            throw DomainError.System.internalServerError("enum data corrupt: {} not exists for {}", v, c);
        }
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
    static <V> @Nullable JsonArray option(Optional<V> v, JsonArrayWriter<V> writer) {
        if (v.isEmpty()) return null;
        JsonArray ar = new JsonArray();
        ar.addNull();
        writer.accept(0, ar, v.get());
        return ar;
    }

    static <V> Optional<V> option(@Nullable JsonArray v, JsonArrayReader<V> act) {
        if (v == null || v.isEmpty()) return Optional.empty();
        return Optional.ofNullable(act.apply(0, v));
    }

    static <V> @Nullable JsonObject jsonMap(@Nullable Map<String, @Nullable V> v, JsonObjectWriter<V> act) {
        if (v == null) return null;
        if (v.isEmpty()) return new JsonObject();
        var m = new JsonObject();
        v.forEach((k, vv) -> act.accept(k, m, vv));
        return m;
    }

    static <V> @Nullable Map<String, @Nullable V> jsonMap(@Nullable JsonObject v, JsonObjectReader<V> act) {
        if (v == null) return null;
        if (v.isEmpty()) return new HashMap<>();
        var m = new HashMap<String, @Nullable V>();
        for (var key : v.getMap().keySet()) {
            m.put(key, act.apply(key, v));
        }
        return m;
    }


    static <T extends Enum<T>> @Nullable Buf enumToBuf(@Nullable T t) {
        return t == null ? null : Buf.of().string(t.name());
    }

    static <T extends Enum<T>> Function<@Nullable Buf, @Nullable T> enumFromBuf(Class<T> type) {
        return b -> b == null ? null : Enum.valueOf(type, Objects.requireNonNull(b.string()));
    }

    static <T extends Data> Function<@Nullable Buf, @Nullable T> fromBuf(Function<JsonObject, T> ctor) {
        return b -> b == null ? null : ctor.apply(b.toJsonObject());
    }

    static <T extends Data> @Nullable Buf toBuf(@Nullable T t) {
        return t == null ? null : Buf.of(t.asJson().toBuffer());
    }

    static <T, E extends Collection<T>> Function<@Nullable Buf, @Nullable E> collectionFromBuf(IntFunction<E> ctor, Function<Optional<Buf>, T> read) {
        return b -> {
            if (b == null) return null;
            return b.repeat(ctor, (bx) -> read.apply(Optional.ofNullable(bx.buf())));
        };
    }

    static <T, E extends Collection<T>> Function<@Nullable E, @Nullable Buf> collectionToBuf(Function<@Nullable T, Buf> write) {
        return e -> {
            if (e == null) return null;
            var b = Buf.of();
            b.repeat(e, (v, bf) -> bf.buf(write.apply(v)));
            return b;
        };
    }

    static <K, V, E extends Map<K, V>> Function<@Nullable Buf, @Nullable E> mapFromBuf(IntFunction<E> ctor, Function<Optional<Buf>, K> kr, Function<Optional<Buf>, V> vr) {
        return b -> {
            if (b == null) return null;
            return b.map(ctor,
                    (bx) -> kr.apply(Optional.ofNullable(bx.buf())),
                    (bx) -> vr.apply(Optional.ofNullable(bx.buf())));
        };
    }

    static <K, V, E extends Map<K, @Nullable V>> Function<@Nullable E, @Nullable Buf> mapToBuf(Function<K, Buf> kw, Function<@Nullable V, Buf> vw) {
        return e -> {
            if (e == null) return null;
            var b = Buf.of();
            b.map(e, (bf, v) -> bf.buf(kw.apply(v)), (v, bf) -> bf.buf(vw.apply(v)));
            return b;
        };
    }


    static @Nullable String time(@Nullable LocalTime v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    static @Nullable LocalTime time(@Nullable String v) {
        return v == null || v.isBlank() ? null : LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse(v));
    }

    static @Nullable String date(@Nullable LocalDate v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    static @Nullable LocalDate date(@Nullable String v) {
        return v == null || v.isBlank() ? null : LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(v));
    }

    static @Nullable String datetime(@Nullable LocalDateTime v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static @Nullable LocalDateTime datetime(@Nullable String v) {
        return v == null || v.isBlank() ? null : LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(v));
    }

    static @Nullable String timeTZ(@Nullable OffsetTime v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_OFFSET_TIME);
    }

    static @Nullable OffsetTime timeTZ(@Nullable String v) {
        return v == null || v.isBlank() ? null : OffsetTime.from(DateTimeFormatter.ISO_OFFSET_TIME.parse(v));
    }

    static @Nullable String datetimeTZ(@Nullable OffsetDateTime v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    static @Nullable OffsetDateTime datetimeTZ(@Nullable String v) {
        return v == null || v.isBlank() ? null : OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(v));
    }

    static @Nullable UUID uuid(@Nullable String v) {
        return v == null || v.isBlank() ? null : UUID.fromString(v);
    }

    static @Nullable String uuid(@Nullable UUID v) {
        return v == null ? null : v.toString();
    }
    //endregion

    //region ToJS
    static <T> void toJs(JsonObject out, BiFunction<JsonObject, String, @Nullable T> value, String... key) {
        T v = null;
        String k = null;
        for (var s : key) {
            v = value.apply(out, s);
            if (v != null) {
                k = s;
                break;
            }
        }
        if (v != null) {
            //! current only process long value
            if (v instanceof Long l) {
                out.put(k, l.toString());
            }
        }
    }

    static <T> void toJs(JsonObject out, BiFunction<JsonObject, String, @Nullable T> value, String key, Supplier<T> def) {
        T v = value.apply(out, key);
        if (v == null) v = def.get();
        if (v instanceof Long l) {
            out.put(key, l.toString());
        }
    }

    static <T> void toJsOpt(JsonObject out, BiFunction<JsonObject, String, @Nullable T> value, String key, Supplier<Optional<T>> def) {
        T v = value.apply(out, key);
        if (v == null) v = def.get().orElse(null);
        if (v instanceof Long l) {
            out.put(key, l.toString());
        }
    }

    static <K, T> void fromJs(JsonObject out, BiFunction<JsonObject, String, @Nullable K> value, Function<K, T> parse, String... key) {
        K v = null;
        String k = null;
        for (var s : key) {
            v = value.apply(out, s);
            if (v != null) {
                k = s;
                break;
            }
        }
        if (v != null) {
            out.put(k, parse.apply(v));
        }
    }

    static <K, T> void fromJs(JsonObject out, BiFunction<JsonObject, String, @Nullable K> value, Function<K, T> parse, String key, Supplier<T> def) {
        K v = value.apply(out, key);
        if (v == null) {
            out.put(key, def.get());
        } else {
            out.put(key, parse.apply(v));
        }

    }

    static <K, T> void fromJsOpt(JsonObject out, BiFunction<JsonObject, String, @Nullable K> value, Function<K, T> parse, String key, Supplier<Optional<T>> def) {
        K v = value.apply(out, key);
        if (v == null) {
            out.put(key, def.get().orElse(null));
        } else {
            out.put(key, parse.apply(v));
        }

    }
    //endregion

    //region binary properties
    BinaryProperty.I<Boolean> BOOLEAN_BINARY = BinaryProperty.nonNull(Buf::bool, Buf::bool);
    BinaryProperty.I<Byte> BYTE_BINARY = BinaryProperty.nonNull(Buf::i8, Buf::i8);
    BinaryProperty.I<Short> SHORT_BINARY = BinaryProperty.nonNull(Buf::i16, Buf::i16);
    BinaryProperty.I<Integer> INT_BINARY = BinaryProperty.nonNull(Buf::i32, Buf::i32);
    BinaryProperty.I<Long> LONG_BINARY = BinaryProperty.nonNull(Buf::i64, Buf::i64);
    BinaryProperty.I<Float> FLOAT_BINARY = BinaryProperty.nonNull(Buf::f32, Buf::f32);
    BinaryProperty.I<Double> DOUBLE_BINARY = BinaryProperty.nonNull(Buf::f64, Buf::f64);
    BinaryProperty.I<Character> CHAR_BINARY = BinaryProperty.nonNull(Buf::character, Buf::character);
    BinaryProperty.I<Void> VOID_OBJECT_BINARY = BinaryProperty.nullable(x -> null, (b, v) -> b);
    BinaryProperty.I<Boolean> BOOLEAN_OBJECT_BINARY = BinaryProperty.nullable(Buf::booleanObject, Buf::booleanObject);
    BinaryProperty.I<Byte> BYTE_OBJECT_BINARY = BinaryProperty.nullable(Buf::byteObject, Buf::byteObject);
    BinaryProperty.I<Short> SHORT_OBJECT_BINARY = BinaryProperty.nullable(Buf::shortObject, Buf::shortObject);
    BinaryProperty.I<Integer> INT_OBJECT_BINARY = BinaryProperty.nullable(Buf::integerObject, Buf::integerObject);
    BinaryProperty.I<Long> LONG_OBJECT_BINARY = BinaryProperty.nullable(Buf::longObject, Buf::longObject);
    BinaryProperty.I<Float> FLOAT_OBJECT_BINARY = BinaryProperty.nullable(Buf::floatObject, Buf::floatObject);
    BinaryProperty.I<Double> DOUBLE_OBJECT_BINARY = BinaryProperty.nullable(Buf::doubleObject, Buf::doubleObject);
    BinaryProperty.I<Character> CHAR_OBJECT_BINARY = BinaryProperty.nullable(Buf::characterObject, Buf::characterObject);
    BinaryProperty.I<String> STRING_BINARY = BinaryProperty.nullable(Buf::string, Buf::string);
    BinaryProperty.I<byte[]> BYTES_BINARY = BinaryProperty.nullable(Buf::binary, Buf::binary);
    BinaryProperty.I<Buffer> BUFFER_BINARY = BinaryProperty.nullable(Buf::buffer, Buf::buffer);
    BinaryProperty.I<Instant> INSTANT_BINARY = LONG_OBJECT_BINARY.map(Fn.nullable(Instant::toEpochMilli), Fn.nullable(Instant::ofEpochMilli));
    BinaryProperty.I<JsonObject> JSON_OBJECT_BINARY = BUFFER_BINARY.map(Fn.nullable(JsonObject::toBuffer), Fn.nullable(Buffer::toJsonObject));
    BinaryProperty.I<JsonArray> JSON_ARRAY_BINARY = BUFFER_BINARY.map(Fn.nullable(JsonArray::toBuffer), Fn.nullable(Buffer::toJsonArray));

    static <T extends Data> Buf binaryData(Buf b, @Nullable T v, @Nullable Class<T> type) {
        if (type == null) return v == null
                ? b.string(null)
                : v instanceof Data.Binary x
                ? b.string(v.getClass().getName()).bool(false).apply(x::toBuf)
                : b.string(v.getClass().getName()).bool(true).buffer(v.toJson().toBuffer());
        return v == null ? b.string(null)
                : v instanceof Data.Binary x
                ? b.string(type.getName()).bool(false).apply(x::toBuf)
                : b.string(type.getName()).bool(true).buffer(v.toJson().toBuffer());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T extends Data> @Nullable T binaryData(Buf b, @Nullable Class<T> type) {
        var clz = b.string();
        if (clz == null) return null;
        var jo = b.bool();
        if (jo) {
            @SuppressWarnings("DataFlowIssue") var o = b.buffer().toJsonObject();
            var clazz = clazz(clz);
            return (T) clazz.getConstructor(JsonObject.class).newInstance(o);
        } else {
            var clazz = clazz(clz);
            return (T) clazz.getConstructor(Buf.class).newInstance(b);
        }
    }

    static <T extends Enum<T>> Buf binaryEnum(Buf b, @Nullable T e) {
        return e == null ? b.v32(-1) : b.v32(e.ordinal());
    }

    static <T extends Enum<T>> @Nullable T binaryEnum(Buf b, Class<T> type) {
        var x = b.v32();
        return x < 0 ? null : type.getEnumConstants()[x];
    }

    static <T extends Enum<T>> Buf binaryEnumText(Buf b, @Nullable T e) {
        return e == null ? b.string("") : b.string(e.name());
    }

    static <T extends Enum<T>> @Nullable T binaryEnumText(Buf b, Class<T> type) {
        var x = b.string();
        return x == null || x.isEmpty() ? null : Enum.valueOf(type, x);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Buf binaryOption(Buf b, Optional<T> v, BiFunction<Buf, @Nullable T, Buf> act) {
        return act.apply(b, v.orElse(null));
    }

    static <T> Optional<T> binaryOption(Buf b, Function<Buf, @Nullable T> act) {
        return Optional.ofNullable(act.apply(b));
    }

    static <T extends Data.Binary> Function<@Nullable Buf, @Nullable T> binaryFromBuf(Function<Buf, T> ctor) {
        return b -> b == null ? null : ctor.apply(b);
    }

    static <T extends Data.Binary> @Nullable Buf binaryToBuf(@Nullable T t) {
        return t == null ? null : t.toBuf(Buf.of());
    }

    //endregion binary

    static <A extends A0, A0 extends Data, B extends B0, B0 extends Data> B convert(A a, Codec.DataCodec<? extends A, A0> ac, Codec.DataCodec<? extends B, B0> bc) {
        return Objects.requireNonNull(bc.get(Objects.requireNonNull(ac.from(a),"missing source data").asJson()),"missing target data");
    }
    static <A extends A0, A0 extends Data, B extends B0, B0 extends Data> Function<A,B> converter( Codec.DataCodec<? extends A, A0> ac, Codec.DataCodec<? extends B, B0> bc) {
        return a->Objects.requireNonNull(bc.get(Objects.requireNonNull(ac.from(a),"missing source data").asJson()),"missing target data");
    }
}
