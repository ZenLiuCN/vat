package vat.api.store;

import io.vertx.sqlclient.Row;
import org.jooq.lambda.tuple.Tuple;

import java.util.List;

import static org.jooq.lambda.tuple.Tuple.tuple;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public record TupleReader(List<ValueReader<?>> reader) implements Reader<Tuple> {

    @Override
    public Tuple read(Row row) {
        Tuple t = tuple();
        for (var i : reader) {
            t = i.append(t, row);
        }
        return t;
    }

}
