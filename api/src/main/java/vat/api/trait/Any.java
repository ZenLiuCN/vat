package vat.api.trait;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import vat.api.Data;
import vat.api.implement.Codec;
import vat.api.meta.Nullable;
import vat.api.utils.Buf;
import vat.api.utils.Fn;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/// Support dynamic scalar type value except container type.
///
/// @author Zen.Liu
/// @since 2025-12-02
public record Any(@Nullable Object value, Class<?> type) implements Data, Data.Binary {
    public Any(Buf v) {
        this(Codec.clazz(v.string()), parseBuf(v));
    }

    Any(@Nullable Class<?> type, Object value) {
        this(value, type);
    }

    public static Buf formatBuf(Buf buf, Object value, Class<?> type) {
        buf.string(type.isPrimitive() ? type.getName() : type.getCanonicalName());
        return switch (value) {
            case null -> buf.bool(false);
            case Data.Binary d -> d.toBuf(buf.bool(true));
            case Data d -> buf.bool(true).buf(Buf.of(d.toJson().toBuffer()));
            case Boolean l -> buf.bool(true).bool(l);
            case Short l -> buf.bool(true).i16(l);
            case Character l -> buf.bool(true).i32(l);
            case Integer l -> buf.bool(true).i32(l);
            case Long l -> buf.bool(true).i64(l);
            case Float l -> buf.bool(true).f32(l);
            case Double l -> buf.bool(true).f64(l);
            case String l -> buf.bool(true).string(l);
            case byte[] l -> buf.bool(true).bytes(l);
            case Buffer l -> buf.bool(true).buffer(l);
            case JsonObject d -> buf.bool(true).buffer(d.toBuffer());
            case JsonArray d -> buf.bool(true).buffer(d.toBuffer());
            case Instant d -> buf.bool(true).i64(d.toEpochMilli());
            case Class<?> d -> buf.bool(true).string(d.isPrimitive() ? d.getName() : d.getCanonicalName());
            case Enum<?> d -> buf.bool(true).string(d.name());
            case BigDecimal d -> buf.bool(true).string(d.toString());
            case LocalDateTime d -> buf.bool(true).string(Codec.datetime(d));
            case LocalDate d -> buf.bool(true).string(Codec.date(d));
            case LocalTime d -> buf.bool(true).string(Codec.time(d));
            case OffsetTime d -> buf.bool(true).string(Codec.timeTZ(d));
            case OffsetDateTime d -> buf.bool(true).string(Codec.datetimeTZ(d));
            case Period d -> buf.bool(true).string(Codec.period(d));
            case Duration d -> buf.bool(true).string(Codec.duration(d));
            case UUID d -> buf.bool(true).string(Codec.uuid(d));
            default -> throw new IllegalArgumentException("not supported value type: " + value);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SneakyThrows
    public static Object parseBuf(Buf buf) {
        buf.reset();
        var clz = (Class) Codec.clazz(buf.string());
        if (!buf.bool()) return null;
        if (clz == null) return null;
        if (clz == boolean.class || clz == Boolean.class) return (Boolean) buf.bool();
        if (clz == byte.class || clz == Byte.class) return (Byte) buf.i8();
        if (clz == short.class || clz == Short.class) return (Short) buf.i16();
        if (clz == int.class || clz == Integer.class) return (Integer) buf.i32();
        if (clz == char.class || clz == Character.class) return (Character) (char) buf.i32();
        if (clz == long.class || clz == Long.class) return (Long) buf.i64();
        if (clz == float.class || clz == Float.class) return (Float) buf.f32();
        if (clz == double.class || clz == Double.class) return (Double) buf.f64();
        if (clz == byte[].class) return buf.binary();
        if (clz == Buffer.class) return buf.buffer();
        if (clz == JsonObject.class) return buf.buffer().toJsonObject();
        if (clz == JsonArray.class) return buf.buffer().toJsonArray();
        if (clz == String.class) return buf.string();
        if (clz == BigDecimal.class) return new BigDecimal(buf.string());
        if (clz == LocalDateTime.class) return Codec.datetime(buf.string());
        if (clz == LocalDate.class) return Codec.date(buf.string());
        if (clz == LocalTime.class) return Codec.time(buf.string());
        if (clz == OffsetTime.class) return Codec.timeTZ(buf.string());
        if (clz == OffsetDateTime.class) return Codec.datetimeTZ(buf.string());
        if (clz == Period.class) return Codec.period(buf.string());
        if (clz == Duration.class) return Codec.duration(buf.string());
        if (clz == UUID.class) return Codec.uuid(buf.string());
        if (clz.isAssignableFrom(Class.class)) return Codec.clazz(buf.string());
        if (clz.isAssignableFrom(Enum.class)) return Codec.enumerateText(buf.string(), clz);
        if (clz == Instant.class) return Instant.ofEpochMilli(buf.i64());
        if (clz.isAssignableFrom(Data.Binary.class)) {
            if (clz.isInterface())
                return Codec.binary((Class<Binary>) clz).orElseThrow(() -> new IllegalArgumentException("unsupported any value: " + clz)).read(buf);
            return clz.getConstructor(Buf.class).newInstance(buf);
        }
        //@formatter:on
        throw new IllegalArgumentException("unsupported any value: " + clz);
    }

    @Override
    public Buf toBuf(Buf buf) {
        return formatBuf(buf, value, type);
    }

    public Any(JsonObject v) {
        this(parseJson(v, false), Codec.clazz(v.getString("$union")));
    }

    public Any(JsonObject v, Void ignore) {
        this(parseJson(v, true), Codec.clazz(v.getString("$union")));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object parseJson(JsonObject v, boolean js) {
        var clz = (Class) Codec.clazz(v.getString("$union"));
        //@formatter:off
        if(clz==null) return null;
        if(clz==boolean.class|| clz == Boolean.class) return v.getBoolean("value");
        if(clz==byte.class||clz==Byte.class) return Optional.ofNullable(v.getNumber("value")).map(Number::byteValue).orElse(null);
        if(clz==short.class||clz==Short.class) return Optional.ofNullable(v.getNumber("value")).map(Number::shortValue).orElse(null);
        if(clz==int.class||clz==Integer.class) return v.getInteger("value");
        if(clz==char.class|| clz == Character.class) return Optional.ofNullable(v.getNumber("value")).map(x->(char)x.intValue()).orElse(null);
        if(clz==long.class||clz==Long.class) return (js?Optional.ofNullable(v.getString("value")).map(Long::parseLong).orElse(null):v.getLong("value"));
        if(clz==float.class|| clz == Float.class) return v.getFloat("value");
        if(clz==double.class|| clz == Double.class) return v.getDouble("value");
        if(clz==byte[].class) return v.getBinary("value");
        if(clz==Buffer.class) return v.getBuffer("value");
        if(clz==JsonObject.class) return v.getJsonObject("value");
        if(clz==JsonArray.class) return v.getJsonArray("value");
        if(clz == String.class) return v.getString("value");
        if(clz == BigDecimal.class) return Optional.ofNullable(v.getString("value")).map(BigDecimal::new).orElse(null);
        if(clz == LocalDateTime.class) return Optional.ofNullable(v.getString("value")).map(Codec::datetime).orElse(null);
        if(clz == LocalDate.class) return Optional.ofNullable(v.getString("value")).map(Codec::date).orElse(null);
        if(clz == LocalTime.class) return Optional.ofNullable(v.getString("value")).map(Codec::time).orElse(null);
        if(clz == OffsetTime.class) return Optional.ofNullable(v.getString("value")).map(Codec::timeTZ).orElse(null);
        if(clz == OffsetDateTime.class) return Optional.ofNullable(v.getString("value")).map(Codec::datetimeTZ).orElse(null);
        if(clz == Period.class) return Optional.ofNullable(v.getString("value")).map(Codec::period).orElse(null);
        if(clz == Duration.class) return Optional.ofNullable(v.getString("value")).map(Codec::duration).orElse(null);
        if(clz == UUID.class) return Optional.ofNullable(v.getString("value")).map(Codec::uuid).orElse(null);
        if(clz.isAssignableFrom(Class.class)) return Optional.ofNullable(v.getString("value")).map(Codec::clazz).orElse(null);
        if(clz.isAssignableFrom(Enum.class)) return Optional.ofNullable(v.getValue("value")).map(x->{
            if(x instanceof String s) return Codec.enumerateText(s,clz);
            else if(x instanceof Number n) return Codec.enumerate(n.intValue(),clz);
            return null;
        }).orElse(null);
        if(clz == Instant.class) return v.getInstant("value");
        if(clz.isAssignableFrom(Data.class))
            return clz.isInterface()?Codec.codec(clz).fromJson(v.getJsonObject("value")):Codec.data(v.getJsonObject("value"),clz);
        //@formatter:on
        throw new IllegalArgumentException("unsupported any value: " + v);
    }

    public static JsonObject formatJson(Object value, Class<?> type, boolean js) {
        var jo = JsonObject.of("$union", type.isPrimitive() ? type.getName() : type.getCanonicalName());
        return switch (value) {
            case null -> jo;
            case Data d -> jo.put("value", d.toJson());
            case Boolean d -> jo.put("value", d);
            case Short d -> jo.put("value", d);
            case Integer d -> jo.put("value", d);
            case Character d -> jo.put("value", d);
            case Long d -> js ? jo.put("value", d.toString()) : jo.put("value", d);
            case Float d -> jo.put("value", d);
            case Double d -> jo.put("value", d);
            case String d -> jo.put("value", d);
            case byte[] d -> jo.put("value", d);
            case Buffer d -> jo.put("value", d);
            case JsonObject d -> jo.put("value", d);
            case JsonArray d -> jo.put("value", d);
            case Instant d -> jo.put("value", d);
            case Class<?> d -> jo.put("value", d.isPrimitive() ? d.getName() : d.getCanonicalName());
            case Enum<?> d -> jo.put("value", d.name());
            case BigDecimal d -> jo.put("value", d.toString());
            case LocalDateTime d -> jo.put("value", Codec.datetime(d));
            case LocalDate d -> jo.put("value", Codec.date(d));
            case LocalTime d -> jo.put("value", Codec.time(d));
            case OffsetTime d -> jo.put("value", Codec.timeTZ(d));
            case OffsetDateTime d -> jo.put("value", Codec.datetimeTZ(d));
            case Period d -> jo.put("value", Codec.period(d));
            case Duration d -> jo.put("value", Codec.duration(d));
            case UUID d -> jo.put("value", Codec.uuid(d));
            default -> throw new IllegalArgumentException("not supported value type: " + value);
        };
    }

    @Override
    public JsonObject asJson() {
        return formatJson(value, type, false);
    }

    @Override
    public JsonObject toJson() {
        return asJson();
    }

    @Override
    public JsonObject toJS() {
        return formatJson(value, type, true);
    }


    public <T> Optional<T> value(Class<T> type) {
        return Optional.ofNullable(value)
                .filter(type::isInstance)
                .map(type::cast);
    }
    public <T,R> Optional<R> when(Class<T> type, Function<T,R> action) {
        return Optional.ofNullable(value)
                .filter(type::isInstance)
                .map(type::cast)
                .map(action);
    }
    public <T,R> Future<Optional<R>> whenFuture(Class<T> type, Function<T,Future<R>> action) {
        return Optional.ofNullable(value)
                .filter(type::isInstance)
                .map(type::cast)
                .map(Fn.Maybe.Flat.optional(action))
                .orElseGet(()->Future.succeededFuture(Optional.empty()));
    }
}
