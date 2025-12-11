package vat.api.store;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.data.Numeric;
import vat.api.utils.ITimes;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public sealed interface Value<T> extends Renderable permits Expr, Field, Field.ObjectField, RawValue,
        Value.BinaryValue, Value.BooleanValue, Value.ComparableValue, Value.EnumValue, Value.JsonValue,
        Value.ObjectValue, Value.StringValue, Value.TemporalValue {
    sealed interface ComparableValue<T> extends Value<T> {
        @SuppressWarnings("unchecked")
        default BooleanValue in(Value<T>... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue notIn(Value<T>... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue in(T... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue notIn(T... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }


        default BooleanValue in(Collection<? super T> v) {
            assert v.size() > 1;
            return new Compare(Compare.Operator.IN, this, v.toArray());
        }


        default BooleanValue notIn(Collection<? super T> v) {
            assert v.size() > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v.toArray());
        }
    }

    default BooleanValue isNull() {
        return new Compare(Compare.Operator.IS_NULL, this);
    }

    default BooleanValue nonNull() {
        return new Compare(Compare.Operator.NON_NULL, this);
    }

    default BooleanValue eq(Value<T> v) {
        return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v);
    }

    default BooleanValue eq(T v) {
        return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v);
    }

    default BooleanValue neq(Value<T> v) {
        return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v);
    }

    default BooleanValue neq(T v) {
        return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v);
    }

    /// method to convert in to or-equals
    default BooleanValue eqAny(Collection<T> v) {
        if (v == null) throw new IllegalStateException("invalid request");
        var s = new HashSet<>(v);
        if (s.size() == 1) return eq(s.iterator().next());
        Value.BooleanValue cond = null;
        for (var ro : s) {
            cond = cond == null ? this.eq(ro) : cond.or(this.eq(ro));
        }
        return cond;
    }

    /// method to convert not in to and-not-equals
    default BooleanValue neqAll(Collection<T> v) {
        if (v == null) throw new IllegalStateException("invalid request");
        var s = new HashSet<>(v);
        if (s.size() == 1) return eq(s.iterator().next());
        Value.BooleanValue cond = null;
        for (var ro : s) {
            cond = cond == null ? this.neq(ro) : cond.and(this.neq(ro));
        }
        return cond;
    }

    non-sealed interface ObjectValue<T> extends Value<T> {

    }

    sealed interface NumberValue<T extends Number> extends ComparableValue<T>
            permits Mathematics, ByteValue, DecimalValue, DoubleValue, FloatValue, IntegerValue, LongValue, NumericValue, ShortValue {

        default BooleanValue between(Value<T> low, Value<T> high) {
            return new Compare(this, low, high);
        }


        default BooleanValue between(T low, T high) {
            return new Compare(this, low, high);
        }


    }

    non-sealed interface NumericValue extends NumberValue<Numeric> {


        final class NumericMathematics extends Mathematics<Numeric> implements NumericValue {

            public NumericMathematics(Value<?> left) {
                super(left, 0);
            }

            public NumericMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, 0);
            }
        }

        default NumericValue plus(NumericValue v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default NumericValue minus(NumericValue v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default NumericValue times(NumericValue v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default NumericValue div(NumericValue v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default NumericValue rem(NumericValue v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default NumericValue neg() {
            return new NumericValue.NumericMathematics(this);
        }

        default NumericValue plus(Numeric v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default NumericValue minus(Numeric v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default NumericValue times(Numeric v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default NumericValue div(Numeric v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default NumericValue rem(Numeric v) {
            return new NumericValue.NumericMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default BooleanValue gt(NumericValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(NumericValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(NumericValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(NumericValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(Numeric v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(Numeric v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(Numeric v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(Numeric v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


    }

    non-sealed interface DecimalValue extends NumberValue<BigDecimal> {

        final class DecimalMathematics extends Mathematics<BigDecimal> implements DecimalValue {

            public DecimalMathematics(Value<?> left) {
                super(left, -1);
            }

            public DecimalMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, -1);
            }
        }

        default DecimalValue plus(DecimalValue v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default DecimalValue minus(DecimalValue v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default DecimalValue times(DecimalValue v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default DecimalValue div(DecimalValue v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default DecimalValue rem(DecimalValue v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default DecimalValue neg() {
            return new DecimalValue.DecimalMathematics(this);
        }

        default DecimalValue plus(BigDecimal v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default DecimalValue minus(BigDecimal v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default DecimalValue times(BigDecimal v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default DecimalValue div(BigDecimal v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default DecimalValue rem(BigDecimal v) {
            return new DecimalValue.DecimalMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default BooleanValue gt(DecimalValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(DecimalValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(DecimalValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(DecimalValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(BigDecimal v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(BigDecimal v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(BigDecimal v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(BigDecimal v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }
    }

    non-sealed interface IntegerValue extends NumberValue<Integer> {

        final class IntegerMathematics extends Mathematics<Integer> implements IntegerValue {

            public IntegerMathematics(Value<?> left) {
                super(left, 32);
            }

            public IntegerMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, 32);
            }
        }

        default BooleanValue in(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(int low, int high) {
            return new Compare(this, low, high);
        }


        default IntegerValue plus(Value<Integer> v) {
            return new IntegerMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default IntegerValue minus(Value<Integer> v) {
            return new IntegerMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default IntegerValue times(Value<Integer> v) {
            return new IntegerMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default IntegerValue div(Value<Integer> v) {
            return new IntegerMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default IntegerValue rem(Value<Integer> v) {
            return new IntegerMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default IntegerValue neg() {
            return new IntegerMathematics(this);
        }

        default IntegerValue plus(int v) {
            return new IntegerMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default IntegerValue minus(int v) {
            return new IntegerMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default IntegerValue times(int v) {
            return new IntegerMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default IntegerValue div(int v) {
            return new IntegerMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default IntegerValue rem(int v) {
            return new IntegerMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        class IntegerBitwise extends Bitwise<Integer> implements IntegerValue {

            IntegerBitwise(Operator op, Value<?> left, Object right) {
                super(op, left, right, 32);
            }

            IntegerBitwise(Value<?> left) {
                super(Operator.NOT, left, null, 32);
            }
        }

        default IntegerValue not() {
            return new IntegerBitwise(this);
        }

        default IntegerValue or(int v) {
            return new IntegerBitwise(Bitwise.Operator.OR, this, v);
        }

        default IntegerValue and(int v) {
            return new IntegerBitwise(Bitwise.Operator.AND, this, v);
        }

        default IntegerValue xor(int v) {
            return new IntegerBitwise(Bitwise.Operator.XOR, this, v);
        }

        default IntegerValue shl(int v) {
            return new IntegerBitwise(Bitwise.Operator.SHL, this, v);
        }

        default IntegerValue shr(int v) {
            return new IntegerBitwise(Bitwise.Operator.SHR, this, v);
        }


        default BooleanValue gt(IntegerValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(IntegerValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(IntegerValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(IntegerValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(int v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(int v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(int v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(int v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


    }

    non-sealed interface FloatValue extends NumberValue<Float> {
        final class FloatMathematics extends Mathematics<Float> implements FloatValue {

            public FloatMathematics(Value<?> left) {
                super(left, -32);
            }

            public FloatMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, -32);
            }
        }

        default BooleanValue in(float... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(float... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(float low, float high) {
            return new Compare(this, low, high);
        }


        default FloatValue plus(Value<Float> v) {
            return new FloatMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default FloatValue minus(Value<Float> v) {
            return new FloatMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default FloatValue times(Value<Float> v) {
            return new FloatMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default FloatValue div(Value<Float> v) {
            return new FloatMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default FloatValue rem(Value<Float> v) {
            return new FloatMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default FloatValue neg() {
            return new FloatMathematics(this);
        }

        default FloatValue plus(float v) {
            return new FloatMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default FloatValue minus(float v) {
            return new FloatMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default FloatValue times(float v) {
            return new FloatMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default FloatValue div(float v) {
            return new FloatMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default FloatValue rem(float v) {
            return new FloatMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default BooleanValue gt(FloatValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(FloatValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(FloatValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(FloatValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(float v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(float v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(float v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(float v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }
    }

    non-sealed interface LongValue extends NumberValue<Long> {


        final class LongMathematics extends Mathematics<Long> implements LongValue {

            public LongMathematics(Value<?> left) {
                super(left, 64);
            }

            public LongMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, 64);
            }
        }

        default BooleanValue in(long... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(long... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(long low, long high) {
            return new Compare(this, low, high);
        }


        default LongValue plus(Value<Long> v) {
            return new LongMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default LongValue minus(Value<Long> v) {
            return new LongMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default LongValue times(Value<Long> v) {
            return new LongMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default LongValue div(Value<Long> v) {
            return new LongMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default LongValue rem(Value<Long> v) {
            return new LongMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default LongValue neg() {
            return new LongMathematics(this);
        }

        default LongValue plus(long v) {
            return new LongMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default LongValue minus(long v) {
            return new LongMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default LongValue times(long v) {
            return new LongMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default LongValue div(long v) {
            return new LongMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default LongValue rem(long v) {
            return new LongMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        class LongBitwise extends Bitwise<Long> implements LongValue {

            LongBitwise(Operator op, Value<?> left, Object right) {
                super(op, left, right, 64);
            }

            LongBitwise(Value<?> left) {
                super(Operator.NOT, left, null, 64);
            }
        }

        default LongValue not() {
            return new LongBitwise(this);
        }

        default LongValue or(long v) {
            return new LongBitwise(Bitwise.Operator.OR, this, v);
        }

        default LongValue and(long v) {
            return new LongBitwise(Bitwise.Operator.AND, this, v);
        }

        default LongValue xor(long v) {
            return new LongBitwise(Bitwise.Operator.XOR, this, v);
        }

        default LongValue shl(long v) {
            return new LongBitwise(Bitwise.Operator.SHL, this, v);
        }

        default LongValue shr(long v) {
            return new LongBitwise(Bitwise.Operator.SHR, this, v);
        }


        default BooleanValue gt(LongValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(LongValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(LongValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(LongValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(long v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(long v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(long v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(long v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }
    }


    non-sealed interface DoubleValue extends NumberValue<Double> {
        final class DoubleMathematics extends Mathematics<Double> implements DoubleValue {

            public DoubleMathematics(Value<?> left) {
                super(left, -64);
            }

            public DoubleMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, -64);
            }
        }

        default BooleanValue in(double... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(double... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(double low, double high) {
            return new Compare(this, low, high);
        }


        default DoubleValue plus(Value<Double> v) {
            return new DoubleMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default DoubleValue minus(Value<Double> v) {
            return new DoubleMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default DoubleValue times(Value<Double> v) {
            return new DoubleMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default DoubleValue div(Value<Double> v) {
            return new DoubleMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default DoubleValue rem(Value<Double> v) {
            return new DoubleMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default DoubleValue neg() {
            return new DoubleMathematics(this);
        }

        default DoubleValue plus(double v) {
            return new DoubleMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default DoubleValue minus(double v) {
            return new DoubleMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default DoubleValue times(double v) {
            return new DoubleMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default DoubleValue div(double v) {
            return new DoubleMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default DoubleValue rem(double v) {
            return new DoubleMathematics(Mathematics.Operator.REMINDER, this, v);
        }


        default BooleanValue gt(DoubleValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(DoubleValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(DoubleValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(DoubleValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(double v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(double v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(double v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(double v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

    }

    non-sealed interface ShortValue extends NumberValue<Short> {
        final class ShortMathematics extends Mathematics<Short> implements ShortValue {

            public ShortMathematics(Value<?> left) {
                super(left, 16);
            }

            public ShortMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, 16);
            }
        }

        default BooleanValue in(short... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(short... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(short low, short high) {
            return new Compare(this, low, high);
        }


        default ShortValue plus(Value<Short> v) {
            return new ShortMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default ShortValue minus(Value<Short> v) {
            return new ShortMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default ShortValue times(Value<Short> v) {
            return new ShortMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default ShortValue div(Value<Short> v) {
            return new ShortMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default ShortValue rem(Value<Short> v) {
            return new ShortMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default ShortValue neg() {
            return new ShortMathematics(this);
        }

        default ShortValue plus(short v) {
            return new ShortMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default ShortValue minus(short v) {
            return new ShortMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default ShortValue times(short v) {
            return new ShortMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default ShortValue div(short v) {
            return new ShortMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default ShortValue rem(short v) {
            return new ShortMathematics(Mathematics.Operator.REMINDER, this, v);
        }


        default BooleanValue gt(ShortValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(ShortValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(ShortValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(ShortValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(short v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(short v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(short v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(short v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }
    }

    non-sealed interface ByteValue extends NumberValue<Byte> {
        final class ByteMathematics extends Mathematics<Byte> implements ByteValue {

            public ByteMathematics(Value<?> left) {
                super(left, 8);
            }

            public ByteMathematics(Operator op, Value<?> left, Object right) {
                super(op, left, right, 8);
            }
        }

        default BooleanValue in(byte... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(byte... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(byte low, byte high) {
            return new Compare(this, low, high);
        }


        default ByteValue plus(Value<Byte> v) {
            return new ByteMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default ByteValue minus(Value<Byte> v) {
            return new ByteMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default ByteValue times(Value<Byte> v) {
            return new ByteMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default ByteValue div(Value<Byte> v) {
            return new ByteMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default ByteValue rem(Value<Byte> v) {
            return new ByteMathematics(Mathematics.Operator.REMINDER, this, v);
        }

        default ByteValue neg() {
            return new ByteMathematics(this);
        }

        default ByteValue plus(byte v) {
            return new ByteMathematics(Mathematics.Operator.PLUS, this, v);
        }

        default ByteValue minus(byte v) {
            return new ByteMathematics(Mathematics.Operator.MINUS, this, v);
        }

        default ByteValue times(byte v) {
            return new ByteMathematics(Mathematics.Operator.TIMES, this, v);
        }

        default ByteValue div(byte v) {
            return new ByteMathematics(Mathematics.Operator.DIVIDE, this, v);
        }

        default ByteValue rem(byte v) {
            return new ByteMathematics(Mathematics.Operator.REMINDER, this, v);
        }


        default BooleanValue gt(ByteValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(ByteValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(ByteValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(ByteValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(byte v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(byte v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(byte v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(byte v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }
    }

    non-sealed interface StringValue extends Value<String> {
        default BooleanValue in(StringValue... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(StringValue... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue eqCaseInsensitive(StringValue v) {
            return new Compare(Compare.Operator.I_EQ, this, v);
        }

        default BooleanValue neqCaseInsensitive(StringValue v) {
            return new Compare(Compare.Operator.I_NONE_EQ, this, v);
        }


        default BooleanValue contains(StringValue v) {
            return new Compare(Compare.Operator.LIKE_CONTAINS, this, v);
        }

        default BooleanValue startsWith(StringValue v) {
            return new Compare(Compare.Operator.LIKE_BEGINS, this, v);
        }

        default BooleanValue endsWith(StringValue v) {
            return new Compare(Compare.Operator.LIKE_ENDS, this, v);
        }

        default BooleanValue containsCaseInsensitive(StringValue v) {
            return new Compare(Compare.Operator.I_LIKE_CONTAINS, this, v);
        }

        default BooleanValue startsWithCaseInsensitive(StringValue v) {
            return new Compare(Compare.Operator.I_LIKE_BEGINS, this, v);
        }

        default BooleanValue endsWithCaseInsensitive(StringValue v) {
            return new Compare(Compare.Operator.I_LIKE_ENDS, this, v);
        }

    }

    non-sealed interface BooleanValue extends Value<Boolean> {
        default BooleanValue isTrue() {
            return new Compare(Compare.Operator.IS_TRUE, this);
        }

        default BooleanValue isFalse() {
            return new Compare(Compare.Operator.IS_FALSE, this);
        }

        @Override
        default BooleanValue neq(Boolean v) {
            return v == null ? nonNull() : new Compare(!v ? Compare.Operator.IS_TRUE : Compare.Operator.IS_FALSE, this);
        }

        default BooleanValue neq(boolean v) {
            return new Compare(!v ? Compare.Operator.IS_TRUE : Compare.Operator.IS_FALSE, this);
        }

        @Override
        default BooleanValue eq(Boolean v) {
            return v == null ? isNull() : new Compare(v ? Compare.Operator.IS_TRUE : Compare.Operator.IS_FALSE, this);
        }

        default BooleanValue eq(boolean v) {
            return new Compare(v ? Compare.Operator.IS_TRUE : Compare.Operator.IS_FALSE, this);
        }

        default BooleanValue and(Value<Boolean> v) {
            return new Logical(Logical.Operator.AND, this, v);
        }

        default BooleanValue or(Value<Boolean> v) {
            return new Logical(Logical.Operator.OR, this, v);
        }

        default BooleanValue and(boolean v) {
            return new Logical(Logical.Operator.AND, this, v);
        }

        default BooleanValue or(boolean v) {
            return new Logical(Logical.Operator.OR, this, v);
        }

        default BooleanValue not() {
            return new Logical(this);
        }
    }

    sealed interface TemporalValue<T extends Temporal> extends Value<T> {
        @SuppressWarnings("unchecked")
        default BooleanValue in(Value<T>... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue notIn(Value<T>... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(Value<T> low, Value<T> high) {
            return new Compare(this, low, high);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue in(T... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        @SuppressWarnings("unchecked")
        default BooleanValue notIn(T... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(T low, T high) {
            return new Compare(this, low, high);
        }

        default BooleanValue gt(Value<T> v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(Value<T> v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(Value<T> v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(Value<T> v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(T v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(T v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(T v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(T v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

    }

    non-sealed interface InstantValue extends TemporalValue<Instant> {


    }

    non-sealed interface DateValue extends TemporalValue<LocalDate> {

    }

    non-sealed interface TimeValue extends TemporalValue<LocalTime> {
    }

    non-sealed interface DateTimeValue extends TemporalValue<LocalDateTime> {
    }

    non-sealed interface DateTimeTZValue extends TemporalValue<OffsetDateTime> {
    }

    non-sealed interface TimeTZValue extends TemporalValue<OffsetTime> {
    }

    sealed interface BinaryValue<T> extends Value<T> {
    }

    non-sealed interface BytesValue extends BinaryValue<byte[]> {
    }

    non-sealed interface BufferValue extends BinaryValue<Buffer> {
    }

    sealed interface EnumValue<T extends Enum<T>> extends Value<T> {
    }

    non-sealed interface EnumOrdinalValue<T extends Enum<T>> extends EnumValue<T> {
        default BooleanValue eqAny(Collection<T> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                HashSet<T> s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;

                    for (T ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.EQ, this, ro.ordinal());
                        cond = cond == null ? cc : cond.or(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue neqAll(Collection<T> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                HashSet<T> s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;
                    for (T ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.NON_EQ, this, ro.ordinal());
                        cond = cond == null ? cc : cond.and(cc);
                    }

                    return cond;
                }
            }
        }
    }

    non-sealed interface EnumTextValue<T extends Enum<T>> extends EnumValue<T> {
    }

    sealed interface JsonValue<T> extends Value<T> {
    }

    non-sealed interface JsonObjectValue extends JsonValue<JsonObject> {
        final class JsonGetJsonObject extends JsonGet<JsonObject, JsonGetJsonObject> implements JsonObjectValue {


            @Override
            protected JsonGetJsonObject self() {
                return this;
            }

            public JsonGetJsonObject(JsonGet<?, ?> root) {
                super(Row::getJsonObject, root);
            }

            public JsonGetJsonObject(JsonValue<?> root) {
                super(Row::getJsonObject, root);
            }
        }

        default JsonObjectValue objectAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonObjectValue.JsonGetJsonObject(j)
                    : new JsonObjectValue.JsonGetJsonObject(this))
                    .at(JsonGet.Type.OBJECT, key);
        }

        default JsonArrayValue arrayAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonArrayValue.JsonGetJsonArray(j)
                    : new JsonArrayValue.JsonGetJsonArray(this))
                    .at(JsonGet.Type.ARRAY, key);
        }

        default BooleanValue booleanAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonBoolean(j)
                    : new JsonGet.JsonBoolean(this))
                    .at(JsonGet.Type.BOOLEAN, key)
                    ;
        }

        default IntegerValue integerAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonInteger(j)
                    : new JsonGet.JsonInteger(this))
                    .at(JsonGet.Type.INTEGER, key)
                    ;
        }

        default LongValue longAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonLong(j)
                    : new JsonGet.JsonLong(this))
                    .at(JsonGet.Type.LONG, key)
                    ;
        }

        default StringValue stringAt(String key) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonString(j)
                    : new JsonGet.JsonString(this))
                    .at(JsonGet.Type.STRING, key)
                    ;
        }
    }

    non-sealed interface JsonArrayValue extends JsonValue<JsonArray> {
        final class JsonGetJsonArray extends JsonGet<JsonArray, JsonGetJsonArray> implements JsonArrayValue {

            @Override
            protected JsonGetJsonArray self() {
                return this;
            }

            public JsonGetJsonArray(JsonGet<?, ?> root) {
                super(Row::getJsonArray, root);
            }

            public JsonGetJsonArray(JsonValue<?> root) {
                super(Row::getJsonArray, root);
            }


        }

        default JsonObjectValue objectAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonObjectValue.JsonGetJsonObject(j)
                    : new JsonObjectValue.JsonGetJsonObject(this))
                    .at(JsonGet.Type.OBJECT, index);
        }

        default JsonArrayValue arrayAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGetJsonArray(j)
                    : new JsonGetJsonArray(this))
                    .at(JsonGet.Type.ARRAY, index);
        }

        default BooleanValue booleanAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonBoolean(j)
                    : new JsonGet.JsonBoolean(this))
                    .at(JsonGet.Type.BOOLEAN, index)
                    ;
        }

        default IntegerValue integerAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonInteger(j)
                    : new JsonGet.JsonInteger(this))
                    .at(JsonGet.Type.INTEGER, index)
                    ;
        }

        default LongValue longAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonLong(j)
                    : new JsonGet.JsonLong(this))
                    .at(JsonGet.Type.LONG, index)
                    ;
        }

        default StringValue stringAt(int index) {
            return (this instanceof JsonGet<?, ?> j
                    ? new JsonGet.JsonString(j)
                    : new JsonGet.JsonString(this))
                    .at(JsonGet.Type.STRING, index)
                    ;
        }
    }

    non-sealed interface UUIDValue extends ComparableValue<UUID> {

        default BooleanValue in(UUID... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }


        default BooleanValue notIn(UUID... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }
    }


    interface LongDatetimeValue extends ObjectValue<ITimes.IDatetime> {
        default BooleanValue eqAny(Collection<ITimes.IDatetime> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;

                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.EQ, this, ro.value());
                        cond = cond == null ? cc : cond.or(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue neqAll(Collection<ITimes.IDatetime> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;
                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.NON_EQ, this, ro.value());
                        cond = cond == null ? cc : cond.and(cc);
                    }

                    return cond;
                }
            }
        }


        default BooleanValue in(long... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(long... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(long low, long high) {
            return new Compare(this, low, high);
        }

        default BooleanValue between(ITimes.IDatetime low, ITimes.IDatetime high) {
            return new Compare(this, low.value(), high.value());
        }

        default BooleanValue eq(Long v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v);
        }

        default BooleanValue neq(Long v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v);
        }

        @Override
        default BooleanValue eq(ITimes.IDatetime v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v.value());
        }

        @Override
        default BooleanValue neq(ITimes.IDatetime v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v.value());
        }

        default BooleanValue gt(LongDatetimeValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(LongDatetimeValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(LongDatetimeValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(LongDatetimeValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(LongValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(LongValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(LongValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(LongValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(long v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(long v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(long v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(long v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(ITimes.IDatetime v) {
            return new Compare(Compare.Operator.GT, this, v.value());
        }

        default BooleanValue gte(ITimes.IDatetime v) {
            return new Compare(Compare.Operator.GTE, this, v.value());
        }

        default BooleanValue lt(ITimes.IDatetime v) {
            return new Compare(Compare.Operator.LT, this, v.value());
        }

        default BooleanValue lte(ITimes.IDatetime v) {
            return new Compare(Compare.Operator.LTE, this, v.value());
        }

        default IntegerValue rem(int value) {
            return new IntegerValue.IntegerMathematics(Mathematics.Operator.REMINDER, this, value);
        }
    }

    interface IntegerDateValue extends ObjectValue<ITimes.IDate> {
        default BooleanValue eqAny(Collection<ITimes.IDate> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;

                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.EQ, this, ro.value());
                        cond = cond == null ? cc : cond.or(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue neqAll(Collection<ITimes.IDate> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;
                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.NON_EQ, this, ro.value());
                        cond = cond == null ? cc : cond.and(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue between(ITimes.IDate low, ITimes.IDate high) {
            return new Compare(this, low.value(), high.value());
        }

        default BooleanValue in(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(int low, int high) {
            return new Compare(this, low, high);
        }

        default BooleanValue eq(Integer v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v);
        }

        default BooleanValue neq(Integer v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v);
        }

        @Override
        default BooleanValue eq(ITimes.IDate v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v.value());
        }

        @Override
        default BooleanValue neq(ITimes.IDate v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v.value());
        }

        default BooleanValue gt(IntegerValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(IntegerValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(IntegerValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(IntegerValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(IntegerDateValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(IntegerDateValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(IntegerDateValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(IntegerDateValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(int v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(int v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(int v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(int v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(ITimes.IDate v) {
            return new Compare(Compare.Operator.GT, this, v.value());
        }

        default BooleanValue gte(ITimes.IDate v) {
            return new Compare(Compare.Operator.GTE, this, v.value());
        }

        default BooleanValue lt(ITimes.IDate v) {
            return new Compare(Compare.Operator.LT, this, v.value());
        }

        default BooleanValue lte(ITimes.IDate v) {
            return new Compare(Compare.Operator.LTE, this, v.value());
        }

        default IntegerValue rem(int value) {
            return new IntegerValue.IntegerMathematics(Mathematics.Operator.REMINDER, this, value);
        }
    }

    interface IntegerTimeValue extends ObjectValue<ITimes.ITime> {
        default BooleanValue eqAny(Collection<ITimes.ITime> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;

                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.EQ, this, ro.value());
                        cond = cond == null ? cc : cond.or(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue neqAll(Collection<ITimes.ITime> v) {
            if (v == null) {
                throw new IllegalStateException("invalid request");
            } else {
                var s = new HashSet<>(v);
                if (s.size() == 1) {
                    return this.eq(s.iterator().next());
                } else {
                    BooleanValue cond = null;
                    for (var ro : s) {
                        var cc = ro == null ? isNull() : new Compare(Compare.Operator.NON_EQ, this, ro.value());
                        cond = cond == null ? cc : cond.and(cc);
                    }

                    return cond;
                }
            }
        }

        default BooleanValue between(ITimes.ITime low, ITimes.ITime high) {
            return new Compare(this, low.value(), high.value());
        }

        default BooleanValue in(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.IN, this, v);
        }

        default BooleanValue notIn(int... v) {
            assert v.length > 1;
            return new Compare(Compare.Operator.NOT_IN, this, v);
        }

        default BooleanValue between(int low, int high) {
            return new Compare(this, low, high);
        }

        default BooleanValue eq(Integer v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v);
        }

        default BooleanValue neq(Integer v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v);
        }

        @Override
        default BooleanValue eq(ITimes.ITime v) {
            return v == null ? isNull() : new Compare(Compare.Operator.EQ, this, v.value());
        }

        @Override
        default BooleanValue neq(ITimes.ITime v) {
            return v == null ? nonNull() : new Compare(Compare.Operator.NON_EQ, this, v.value());
        }

        default BooleanValue gt(IntegerValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(IntegerValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(IntegerValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(IntegerValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(IntegerTimeValue v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(IntegerTimeValue v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(IntegerTimeValue v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(IntegerTimeValue v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }


        default BooleanValue gt(int v) {
            return new Compare(Compare.Operator.GT, this, v);
        }

        default BooleanValue gte(int v) {
            return new Compare(Compare.Operator.GTE, this, v);
        }

        default BooleanValue lt(int v) {
            return new Compare(Compare.Operator.LT, this, v);
        }

        default BooleanValue lte(int v) {
            return new Compare(Compare.Operator.LTE, this, v);
        }

        default BooleanValue gt(ITimes.ITime v) {
            return new Compare(Compare.Operator.GT, this, v.value());
        }

        default BooleanValue gte(ITimes.ITime v) {
            return new Compare(Compare.Operator.GTE, this, v.value());
        }

        default BooleanValue lt(ITimes.ITime v) {
            return new Compare(Compare.Operator.LT, this, v.value());
        }

        default BooleanValue lte(ITimes.ITime v) {
            return new Compare(Compare.Operator.LTE, this, v.value());
        }

        default IntegerValue rem(int value) {
            return new IntegerValue.IntegerMathematics(Mathematics.Operator.REMINDER, this, value);
        }
    }
}
