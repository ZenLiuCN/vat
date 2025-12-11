package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.lambda.tuple.Tuple2;
import vat.api.Data;
import vat.api.implement.ReaderWriter;

import java.util.List;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public
record ObjectReader<T extends Data>(ReaderWriter.Creator<T> creator,
                                    List<Tuple2<String, ValueReader<?>>> reader) implements Reader<T> {
    @Override
    public T read(Row row) {
        var jo = new JsonObject();
        var i = 0;
        for (var r : reader) {
            r.v2.set(jo, r.v1, row, i);
            i++;
        }
        return creator.fromJson(jo);
    }
}
