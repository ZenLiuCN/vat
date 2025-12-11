package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public interface Reader<T> {
    static Reader<?> combine(Function<JsonObject, ?> creator, Field<?>[] fields) {
        return r -> {
            var jo=new  JsonObject();
            var i=0;
            for(Field<?> f : fields){
                f._reader().set(jo,f._property(),r,i);
                i++;
            }
            return creator.apply(jo);
        };
    }

    T read(Row row);

    default <R> Reader<R> map(Function<T, R> mapper) {
        return r -> mapper.apply(read(r));
    }
}
