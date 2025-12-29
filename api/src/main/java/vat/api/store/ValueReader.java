package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.lambda.tuple.*;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public
interface ValueReader<T extends @Nullable Object> {

    record Mapped<T extends @Nullable Object, R extends @Nullable Object>(
            ValueReader<@Nullable T> raw,
            Function<@Nullable T, @Nullable R> map)
            implements ValueReader<R> {

        @Override
        public @Nullable R read(Row row, int index) {
            T r = raw.read(row, index);
            return r == null ? null : map.apply(r);
        }

        @Override
        public void set(JsonObject out, String key, Row row, int index) {
            out.put(key, raw.read(row, index));
        }
    }

    @Nullable T read(Row row, int index);

    default void set(JsonObject out, String key, Row row, int index) {
        out.put(key, read(row, index));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default Tuple append(Tuple out, Row row) {
        return switch (out) {
            case Tuple0 t0 -> t0.concat(read(row, 0));
            case Tuple1 t1 -> t1.concat(read(row, 1));
            case Tuple2 t2 -> t2.concat(read(row, 2));
            case Tuple3 t3 -> t3.concat(read(row, 3));
            case Tuple4 t4 -> t4.concat(read(row, 4));
            case Tuple5 t5 -> t5.concat(read(row, 5));
            case Tuple6 t6 -> t6.concat(read(row, 6));
            case Tuple7 t7 -> t7.concat(read(row, 7));
            case Tuple8 t8 -> t8.concat(read(row, 8));
            case Tuple9 t9 -> t9.concat(read(row, 9));
            case Tuple10 t10 -> t10.concat(read(row, 10));
            case Tuple11 t11 -> t11.concat(read(row, 11));
            case Tuple12 t12 -> t12.concat(read(row, 12));
            case Tuple13 t13 -> t13.concat(read(row, 13));
            case Tuple14 t14 -> t14.concat(read(row, 14));
            case Tuple15 t15 -> t15.concat(read(row, 15));
            case null, default -> throw new IllegalStateException("can't append tuple more than 15");
        };
    }

    default <R> ValueReader<R> map(Function<T, @Nullable R> mapper) {
        return (r, i) -> Optional.ofNullable(read(r, i)).map(mapper).orElse(null);
    }


    default Reader<T> toReader(int index) {
        return r -> read(r, index);
    }
}
