package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public interface Reader<T extends @Nullable Object> {
    static Reader<?> combine(Function<JsonObject, ?> creator, Field<?>[] fields) {
        return r -> {
            var jo = new JsonObject();
            var i = 0;
            for (Field<?> f : fields) {
                f._reader().set(jo, f._property(), r, i);
                i++;
            }
            return creator.apply(jo);
        };
    }

    @Nullable T read(Row row);

    default <R> Reader<R> map(Function<T, R> mapper) {
        return r -> Optional.ofNullable(read(r)).map(mapper).orElse(null);
    }
}
