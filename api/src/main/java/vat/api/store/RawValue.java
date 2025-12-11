package vat.api.store;


import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.data.Numeric;

import java.math.BigDecimal;
import java.time.*;

///
/// @author Zen.Liu
/// @since 2025-10-24


public sealed interface RawValue<T> extends Value<T> {
    String code();

    @Override
    default void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.rawValue(w, this);
    }


    record BooleanRaw(String code) implements RawValue<Boolean>, BooleanValue {
    }

    static BooleanRaw Boolean(String code) {
        return new BooleanRaw(code);
    }

    record JsonArrayRaw(String code) implements RawValue<JsonArray>, JsonArrayValue {
    }

    static JsonArrayRaw JsonArray(String code) {
        return new JsonArrayRaw(code);
    }

    record JsonObjectRaw(String code) implements RawValue<JsonObject>, JsonObjectValue {
    }

    static JsonObjectRaw JsonObject(String code) {
        return new JsonObjectRaw(code);
    }

    record EnumTextRaw<T extends Enum<T>>(Class<T> type, String code) implements RawValue<T>, EnumTextValue<T> {
    }

    static <T extends Enum<T>> EnumTextRaw<T> EnumText(Class<T> type, String code) {
        return new EnumTextRaw<>(type, code);
    }

    record EnumOrdinalRaw<T extends Enum<T>>(Class<T> type, String code) implements RawValue<T>, EnumOrdinalValue<T> {
    }

    static <T extends Enum<T>> EnumOrdinalRaw<T> EnumOrdinal(Class<T> type, String code) {
        return new EnumOrdinalRaw<>(type, code);
    }

    record BufferRaw(String code) implements RawValue<Buffer>, BufferValue {
    }

    static BufferRaw Buffer(String code) {
        return new BufferRaw(code);
    }

    record BytesRaw(String code) implements RawValue<byte[]>, BytesValue {
    }

    static BytesRaw Bytes(String code) {
        return new BytesRaw(code);
    }

    record TimeTZRaw(String code) implements RawValue<OffsetTime>, TimeTZValue {
    }

    static TimeTZRaw TimeTZ(String code) {
        return new TimeTZRaw(code);
    }

    record DateTimeTZRaw(String code) implements RawValue<OffsetDateTime>, DateTimeTZValue {
    }

    static DateTimeTZRaw DateTimeTZ(String code) {
        return new DateTimeTZRaw(code);
    }

    record DateTimeRaw(String code) implements RawValue<LocalDateTime>, DateTimeValue {
    }

    static DateTimeRaw DateTime(String code) {
        return new DateTimeRaw(code);
    }

    record TimeRaw(String code) implements RawValue<LocalTime>, TimeValue {
    }

    static TimeRaw Time(String code) {
        return new TimeRaw(code);
    }

    record DateRaw(String code) implements RawValue<LocalDate>, DateValue {
    }

    static DateRaw Date(String code) {
        return new DateRaw(code);
    }

    record InstantRaw(String code) implements RawValue<Instant>, InstantValue {
    }

    static InstantRaw Instant(String code) {
        return new InstantRaw(code);
    }

    record StringRaw(String code) implements RawValue<String>, StringValue {
    }

    static StringRaw String(String code) {
        return new StringRaw(code);
    }

    record ByteRaw(String code) implements RawValue<Byte>, ByteValue {
    }

    static ByteRaw Byte(String code) {
        return new ByteRaw(code);
    }

    record ShortRaw(String code) implements RawValue<Short>, ShortValue {
    }

    static ShortRaw Short(String code) {
        return new ShortRaw(code);
    }

    record DoubleRaw(String code) implements RawValue<Double>, DoubleValue {
    }

    static DoubleRaw Double(String code) {
        return new DoubleRaw(code);
    }

    record LongRaw(String code) implements RawValue<Long>, LongValue {
    }

    static LongRaw Long(String code) {
        return new LongRaw(code);
    }

    record FloatRaw(String code) implements RawValue<Float>, FloatValue {
    }

    static FloatRaw Float(String code) {
        return new FloatRaw(code);
    }

    record IntegerRaw(String code) implements RawValue<Integer>, IntegerValue {
    }

    static IntegerRaw Integer(String code) {
        return new IntegerRaw(code);
    }

    record DecimalRaw(String code) implements RawValue<BigDecimal>, DecimalValue {
    }

    static DecimalRaw Decimal(String code) {
        return new DecimalRaw(code);
    }

    record NumericRaw(String code) implements RawValue<Numeric>, NumericValue {
    }

    static NumericRaw Numeric(String code) {
        return new NumericRaw(code);
    }

    InstantRaw NOW=new InstantRaw("NOW()");
/*    static void main(String[] args) {
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(org.jooq.lambda.Seq.seq(Value.class.getDeclaredClasses())
                .filter(x -> !x.isSealed())
                .map(x -> """
                         record %1$sRaw(String code) implements RawValue<%1$s>,%1$sValue {}
                         static %1$sRaw %1$s(String code){
                            return new %1$sRaw(code);
                         }
                        """.formatted(x.getSimpleName().substring(0, x.getSimpleName().indexOf("Value"))))
                .toString("\n")), null);
    }*/
}
