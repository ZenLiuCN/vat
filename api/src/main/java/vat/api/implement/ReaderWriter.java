package vat.api.implement;

import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;

/// @author Zen.Liu
/// @since 2025-11-09

public interface ReaderWriter {

    interface DataReader<T extends @Nullable Object> {
        @Nullable T get(@Nullable JsonObject o);
    }

    interface DataWriter<T extends @Nullable Object> {
        @Nullable JsonObject set(@Nullable T t);
    }

    interface Creator<I> {
        @Nullable I fromJson(@Nullable JsonObject json);
    }


}
