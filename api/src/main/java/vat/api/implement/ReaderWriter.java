package vat.api.implement;

import io.vertx.core.json.JsonObject;

/// @author Zen.Liu
/// @since 2025-11-09

public interface ReaderWriter {

    interface DataReader<T> {
        T get(JsonObject o);
    }

    interface DataWriter<T> {
        JsonObject set(T t);
    }

    interface Creator<I> {
        I fromJson(JsonObject json);
    }


}
