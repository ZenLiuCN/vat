package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import java.math.BigDecimal;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-11-03


@SuppressWarnings("unused")
public interface Pointer {


    record pointer(JsonPointer pointer) implements Pointer {
    }

    static Pointer of(String pointer) {
        return new pointer(JsonPointer.from(pointer));
    }
    static Pointer of(JsonPointer pointer) {
        return new pointer(pointer);
    }

    JsonPointer pointer();
    default Optional<Object> getValue(Object json){
        return Optional.ofNullable(pointer().queryJson(json));
    }
    default <T> Optional<T> get(Object json, Class<T> clazz) {
        return Optional.ofNullable(pointer().queryJson(json))
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }
    default Optional<JsonArray> getArray(Object json) {
        return get(json, JsonArray.class);
    }
    default Optional<JsonObject> getObject(Object json) {
        return get(json, JsonObject.class);
    }
    default Optional<String > getString(Object json) {
        return get(json, String.class);
    }
    default Optional<Boolean> getBoolean(Object json) {
        return get(json, Boolean.class);
    }
    default Optional<Integer> getInteger(Object json) {
        return get(json, Number.class).map(Number::intValue);
    }
    default Optional<Short> getShort(Object json) {
        return get(json, Number.class).map(Number::shortValue);
    }
    default Optional<Character> getChar(Object json) {
        return get(json, Number.class).map(Number::intValue).map(x->(char)(int)x);
    }
    default Optional<Byte> getByte(Object json) {
        return get(json, Number.class).map(Number::byteValue);
    }
    default Optional<Long> getLong(Object json) {
        return get(json, Number.class).map(Number::longValue);
    }
    default Optional<Float> getFloat(Object json) {
        return get(json, Number.class).map(Number::floatValue);
    }
    default Optional<Double> getDouble(Object json) {
        return get(json, Number.class).map(Number::doubleValue);
    }
    default Optional<BigDecimal> getDecimal(Object json) {
        return get(json, Double.class).map(BigDecimal::valueOf)
                .or(()->get(json, String.class).map(BigDecimal::new));
    }
    default Optional<byte[]> getBinary(Object json) {
        return get(json, byte[].class)
                .or(()->getString(json).map(JSON.BASE64_DECODER::decode));
    }
    default Optional<Buffer> getBuffer(Object json) {
        return  get(json, Buffer.class)
                .or(()->getBinary(json).map(Buffer::buffer));
    }
}
