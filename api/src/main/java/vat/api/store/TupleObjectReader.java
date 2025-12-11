package vat.api.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.lambda.tuple.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.jooq.lambda.tuple.Tuple.tuple;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public record TupleObjectReader(List<TupleValueReader<?>> reader) implements Reader<Tuple> {

    @Override
    public Tuple read(Row row) {
        Tuple t = tuple();
        var start = new int[]{0};
        for (var i : reader) {
            t = i.append(t, row, start);
        }
        return t;
    }

    interface TupleValueReader<T> {
        static <T> TupleValueReader<T> from(Model<T> model) {
            var ctor = model._create();
            List<Tuple2<String, ValueReader<?>>> fields = Arrays
                    .stream(model._fields())
                    .map(x -> Tuple.<String, ValueReader<?>>tuple(x._property(), x._reader()))
                    .toList();
            return new reader<>(ctor, fields);
        }

        record reader<T>(Function<JsonObject, T> ctor, List<Tuple2<String, ValueReader<?>>> fields) implements
                                                                                                    TupleValueReader<T> {


            @Override
            public T read(Row row, int[] start) {
                var s = start[0];
                var jo = new JsonObject();
                for (var field : fields) {
                    jo.put(field.v1, field.v2.read(row, s));
                    s++;
                }
                start[0] = s;
                return ctor.apply(jo);
            }
        }

        T read(Row row, int[] start);

        @SuppressWarnings({"unchecked", "rawtypes"})
        default Tuple append(Tuple out, Row row, int[] start) {
            if (out instanceof Tuple0 t0) return t0.concat(read(row, start));
            if (out instanceof Tuple1 t1) return t1.concat(read(row, start));
            if (out instanceof Tuple2 t2) return t2.concat(read(row, start));
            if (out instanceof Tuple3 t3) return t3.concat(read(row, start));
            if (out instanceof Tuple4 t4) return t4.concat(read(row, start));
            if (out instanceof Tuple5 t5) return t5.concat(read(row, start));
            if (out instanceof Tuple6 t6) return t6.concat(read(row, start));
            if (out instanceof Tuple7 t7) return t7.concat(read(row, start));
            if (out instanceof Tuple8 t8) return t8.concat(read(row, start));
            if (out instanceof Tuple9 t9) return t9.concat(read(row, start));
            if (out instanceof Tuple10 t10) return t10.concat(read(row, start));
            if (out instanceof Tuple11 t11) return t11.concat(read(row, start));
            if (out instanceof Tuple12 t12) return t12.concat(read(row, start));
            if (out instanceof Tuple13 t13) return t13.concat(read(row, start));
            if (out instanceof Tuple14 t14) return t14.concat(read(row, start));
            if (out instanceof Tuple15 t15) return t15.concat(read(row, start));
            throw new IllegalStateException("can't append tuple more than 15");
        }
    }
}
