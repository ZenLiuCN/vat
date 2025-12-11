package vat.api.utils;

import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.tuple.Tuple2;
import vat.api.Entity;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Identifier {

    static Tuple2<String, String> split(String identifier) {
        if (identifier.isBlank()) return null;
        var i = "";
        var v = "";
        var x = identifier.length() - 1;
        var f = identifier.charAt(x);
        if (identifier.charAt(0) > 'f') i = null;
        if (identifier.charAt(x - 1) > 'f') v = null;
        if (i == null && v == null) return new Tuple2<>(null, null);
        var n = f > 'f' ? 1 : Integer.parseInt(f + "", 16);
        var s = x - n;
        i = i == null ? null : s < 0 ? null : identifier.substring(0, s);
        v = v == null ? null : s < 0 ? null : identifier.substring(s, x);
        return new Tuple2<>(i, v);
    }

    static <I extends Number, V extends Number> String combine(I i,
                                                               V v,
                                                               Function<I, String> a,
                                                               Function<V, String> b
    ) {

        var x = i == null ? NULL1 : i.longValue() == 0 ? "0" : a.apply(i);
        var y = v == null ? NULL2 : v.longValue() == 0 ? "0" : b.apply(v);
        var f = Objects.equals(y, NULL2) ? NULL3 : Integer.toHexString(y.length());
        return x + y + f;
    }


    String FLAGS = "ghijkmnopqrstuvwxuyz";
    char NULL0 = FLAGS.charAt(new Random().nextInt(20 - 3));
    String NULL1 = NULL0 + "";
    String NULL2 = (char) (NULL0 + 1) + "";
    String NULL3 = (char) (NULL0 + 2) + "";

    static String combine(Long i, Long v) {
        return combine(i, v, Long::toHexString, Long::toHexString);
    }

    static String combine(Long i, Integer v) {
        return combine(i, v, Long::toHexString, Integer::toHexString);
    }

    static String combine(Integer i, Integer v) {
        return combine(i, v, Integer::toHexString, Integer::toHexString);
    }

    static Optional<Tuple2<Long, Integer>> parse84(@Nullable String identifier) {
        return Optional.ofNullable(identifier)
                .filter(Predicate.not(String::isBlank))
                .map(Identifier::split)
                .map(v -> v
                        .map1(x -> x == null ? null : Long.parseLong(x, 16))
                        .map2(x -> x == null ? null : Integer.parseInt(x, 16))
                );
    }

    static Optional<Tuple2<Long, Long>> parse88(@Nullable String identifier) {
        return Optional.ofNullable(identifier)
                .filter(Predicate.not(String::isBlank))
                .map(Identifier::split)
                .map(v -> v
                        .map1(x -> x == null ? null : Long.parseLong(x, 16))
                        .map2(x -> x == null ? null : Long.parseLong(x, 16))
                );
    }

    static Optional<Tuple2<Integer, Integer>> parse44(@Nullable String identifier) {
        return Optional.ofNullable(identifier)
                .filter(Predicate.not(String::isBlank))
                .map(Identifier::split)
                .map(v -> v
                        .map1(x -> x == null ? null : Integer.parseInt(x, 16))
                        .map2(x -> x == null ? null : Integer.parseInt(x, 16))
                );
    }

    static Optional<Entity.Entry> entry(@Nullable String identifier) {
        return parse84(identifier)
                .map(v -> Entity.Entry.of(v.v1, v.v2));

    }

    static Optional<String> entry(@Nullable Entity.Entry entry) {
        return Optional.ofNullable(entry)
                .map(x -> combine(x.id(), x.version()));

    }

}
