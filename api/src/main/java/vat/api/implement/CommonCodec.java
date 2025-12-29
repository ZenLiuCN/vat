package vat.api.implement;

import com.google.auto.service.AutoService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;
import vat.api.Entity;
import vat.api.implement.Codec.BinaryProperty;
import vat.api.implement.Codec.CombineProperty;
import vat.api.implement.Codec.DataProperty;
import vat.api.utils.Buf;
import vat.api.utils.Fn;
import vat.api.utils.ITimes;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static vat.api.implement.Codec.list;

///
/// @author Zen.Liu
/// @since 2025-10-30

@AutoService(Codec.Provider.class)
public final class CommonCodec implements Codec.Provider {


    public static final DataProperty<Entity.Entry> ENTRY = new CombineProperty<>(
            (o, k) -> Optional.ofNullable(o.getJsonArray(k)).map(j -> new Entity.Entry.entry(j.getLong(0), j.getInteger(1))).orElse(null),
            (o, k, v) -> o.put(k, v == null ? null : JsonArray.of(v.id(), v.version())));
    public static final BinaryProperty<Entity.Entry> ENTRY_BINARY = BinaryProperty.nullable(
            buf -> buf.bool() ? new Entity.Entry.entry(buf.object(Buf::i64), buf.i32()) : null,
            (buf, e) -> {
                if (e == null) return buf.bool(false);
                return buf.bool(true).object(e.id(), Buf.I64).i32(e.version());
            });

    public static final DataProperty<ITimes.IDate> I_DATE = new CombineProperty<>(
            (o, k) -> Optional.ofNullable(o.getInteger(k)).map(ITimes.IDate::new).orElse(null),
            (o, k, v) -> o.put(k, v == null ? null : v.value()));
    public static final BinaryProperty<ITimes.IDate> I_DATE_BINARY = Codec.INT_OBJECT_BINARY.map(ITimes.IDate::value, ITimes.IDate::new);

    public static final DataProperty<ITimes.ITime> I_TIME = new CombineProperty<>(
            (o, k) -> Optional.ofNullable(o.getInteger(k)).map(ITimes.ITime::new).orElse(null),
            (o, k, v) -> o.put(k, v == null ? null : v.value()));
    public static final BinaryProperty<ITimes.ITime> I_TIME_BINARY = Codec.INT_OBJECT_BINARY.map(ITimes.ITime::value, ITimes.ITime::new);

    public static final DataProperty<ITimes.IDatetime> I_DATETIME = new CombineProperty<>(
            (o, k) -> Optional.ofNullable(o.getLong(k)).map(ITimes.IDatetime::new).orElse(null),
            (o, k, v) -> o.put(k, v == null ? null : v.value()));
    public static final BinaryProperty<ITimes.IDatetime> I_DATETIME_BINARY = Codec.LONG_BINARY.map(ITimes.IDatetime::value, ITimes.IDatetime::new);

    public static final DataProperty<List<@Nullable String>> LIST_$$STRING = new CombineProperty<>(
            (o, k) -> list(o.getJsonArray(k), ArrayList::new, (p_k, r_k) -> r_k.getString(p_k)),
            (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k))));
    public static final BinaryProperty<List<@Nullable String>> LIST_$$STRING_BINARY = BinaryProperty.nullable(
            (o) -> o.repeat(ArrayList::new, (Function<Buf, String>) Buf::string),
            (o, v) -> o.repeat(v, (s, b) -> b.string(s))
    );

    public static final Codec.DataProperty<OffsetDateTime> JAVA_TIME__OFFSET_DATE_TIME = new Codec.CombineProperty<>(
            (o, k) -> Codec.datetimeTZ(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.datetimeTZ(v)));
    public static final BinaryProperty<OffsetDateTime> JAVA_TIME__OFFSET_DATE_TIME_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::datetimeTZ),Fn.nonNull( Codec::datetimeTZ));

    public static final Codec.DataProperty<OffsetTime> JAVA_TIME__OFFSET_TIME = new Codec.CombineProperty<>(
            (o, k) -> Codec.timeTZ(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.timeTZ(v)));
    public static final BinaryProperty<OffsetTime> JAVA_TIME__OFFSET_TIME_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::timeTZ), Fn.nonNull(Codec::timeTZ));

    public static final Codec.DataProperty<LocalDateTime> JAVA_TIME__LOCAL_DATE_TIME = new Codec.CombineProperty<>(
            (o, k) -> Codec.datetime(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.datetime(v)));
    public static final BinaryProperty<LocalDateTime> JAVA_TIME__LOCAL_DATE_TIME_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::datetime), Fn.nonNull(Codec::datetime));

    public static final Codec.DataProperty<LocalDate> JAVA_TIME__LOCAL_DATE = new Codec.CombineProperty<>(
            (o, k) -> Codec.date(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.date(v)));
    public static final BinaryProperty<LocalDate> JAVA_TIME__LOCAL_DATE_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::date), Fn.nonNull(Codec::date));

    public static final Codec.DataProperty<LocalTime> JAVA_TIME__LOCAL_TIME = new Codec.CombineProperty<>(
            (o, k) -> Codec.time(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.time(v)));
    public static final BinaryProperty<LocalTime> JAVA_TIME__LOCAL_TIME_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::time), Fn.nonNull(Codec::time));

    public static final Codec.DataProperty<UUID> UUID = new Codec.CombineProperty<>(
            (o, k) -> Codec.uuid(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.uuid(v)));
    public static final BinaryProperty<UUID> UUID_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::uuid), Fn.nonNull(Codec::uuid));

    public static final Codec.DataProperty<Class<?>> CLASS = new Codec.CombineProperty<>(
            (o, k) -> Codec.clazz(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.clazz(v)));
    public static final BinaryProperty<Class<?>> CLASS_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::clazz), Fn.nonNull(Codec::clazz));

    public static final DataProperty<Duration> JAVA_TIME__DURATION = new CombineProperty<>(
            (o, k) -> Codec.duration(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.duration(v)));
    public static final BinaryProperty<Duration> JAVA_TIME__DURATION_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::duration), Fn.nonNull(Codec::duration));

    public static final DataProperty<Period> JAVA_TIME__PERIOD = new CombineProperty<>(
            (o, k) -> Codec.period(o.getString(k)),
            (o, k, v) -> o.put(k, Codec.period(v)));
    public static final BinaryProperty<Period> JAVA_TIME__PERIOD_BINARY = Codec.STRING_BINARY.map(Fn.nonNull(Codec::period), Fn.nonNull(Codec::period));


    public static final DataProperty<BigDecimal> JAVA_MATH__BIG_DECIMAL = new CombineProperty<>(
            (o, k) -> Optional.ofNullable(o.getString(k)).map(BigDecimal::new).orElse(null),
            JsonObject::put);
    public static final BinaryProperty<BigDecimal> JAVA_MATH__BIG_DECIMAL_BINARY = Codec.STRING_BINARY.map(BigDecimal::toString, BigDecimal::new);

    public static final DataProperty<double[]> DOUBLE$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.doubleArray(o.getJsonArray(k)),
            (o, k, v) -> o.put(k, Codec.doubleArray(v)));
    public static final BinaryProperty<double[]> DOUBLE$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::f64Array, Buf::f64Array);

    public static final DataProperty<boolean[]> BOOLEAN$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.booleanArray(o.getBinary(k)),
            (o, k, v) -> o.put(k, Codec.booleanArray(v)));
    public static final BinaryProperty<boolean[]> BOOLEAN$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::boolArray, Buf::boolArray);

    public static final DataProperty<float[]> FLOAT$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.floatArray(o.getJsonArray(k)),
            (o, k, v) -> o.put(k, Codec.floatArray(v)));
    public static final BinaryProperty<float[]> FLOAT$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::f32Array, Buf::f32Array);


    public static final DataProperty<long[]> LONG$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.longArray(o.getJsonArray(k)),
            (o, k, v) -> o.put(k, Codec.longArray(v)));
    public static final BinaryProperty<long[]> LONG$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::i64Array, Buf::i64Array);

    public static final DataProperty<int[]> INT$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.intArray(o.getJsonArray(k)),
            (o, k, v) -> o.put(k, Codec.intArray(v)));
    public static final BinaryProperty<int[]> INT$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::i32Array, Buf::i32Array);

    public static final DataProperty<short[]> SHORT$$_$ARRAY = new CombineProperty<>(
            (o, k) -> Codec.shortArray(o.getJsonArray(k)),
            (o, k, v) -> o.put(k, Codec.shortArray(v)));
    public static final BinaryProperty<short[]> SHORT$$_$ARRAY_BINARY = BinaryProperty.nullable(Buf::i16Array, Buf::i16Array);


}
