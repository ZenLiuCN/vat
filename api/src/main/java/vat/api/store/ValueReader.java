package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.lambda.tuple.*;

import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public
interface ValueReader<T> {

    record Mapped<T, R>(ValueReader<T> raw, Function<T, R> map) implements ValueReader<R> {

        @Override
        public R read(Row row, int index) {
            var r = raw.read(row, index);
            return r == null ? null : map.apply(r);
        }

        @Override
        public void set(JsonObject out, String key, Row row, int index) {
            out.put(key, raw.read(row, index));
        }
    }

    T read(Row row, int index);

    default void set(JsonObject out, String key, Row row, int index) {
        out.put(key, read(row, index));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default Tuple append(Tuple out, Row row) {
        if (out instanceof Tuple0 t0) return t0.concat(read(row, 0));
        if (out instanceof Tuple1 t1) return t1.concat(read(row, 1));
        if (out instanceof Tuple2 t2) return t2.concat(read(row, 2));
        if (out instanceof Tuple3 t3) return t3.concat(read(row, 3));
        if (out instanceof Tuple4 t4) return t4.concat(read(row, 4));
        if (out instanceof Tuple5 t5) return t5.concat(read(row, 5));
        if (out instanceof Tuple6 t6) return t6.concat(read(row, 6));
        if (out instanceof Tuple7 t7) return t7.concat(read(row, 7));
        if (out instanceof Tuple8 t8) return t8.concat(read(row, 8));
        if (out instanceof Tuple9 t9) return t9.concat(read(row, 9));
        if (out instanceof Tuple10 t10) return t10.concat(read(row, 10));
        if (out instanceof Tuple11 t11) return t11.concat(read(row, 11));
        if (out instanceof Tuple12 t12) return t12.concat(read(row, 12));
        if (out instanceof Tuple13 t13) return t13.concat(read(row, 13));
        if (out instanceof Tuple14 t14) return t14.concat(read(row, 14));
        if (out instanceof Tuple15 t15) return t15.concat(read(row, 15));
        throw new IllegalStateException("can't append tuple more than 15");
    }

    default <R> ValueReader<R> map(Function<T, R> mapper) {
        return (r, i) -> mapper.apply(read(r, i));
    }


    default Reader<T> toReader(int index) {
        return r -> read(r, index);
    }
}
