package vat.api.store;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.data.Numeric;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import vat.api.implement.Codec;
import vat.api.utils.Fn;
import vat.api.utils.ITimes;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static vat.api.utils.Fn.nullable;


/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public sealed interface Field<T extends @Nullable Object> extends Value<T>, Renderable {
    record HistoryProxy(Field<?> raw) implements Field<JsonObject> {

        @Override
        public @Nullable Function<Object, Object> _onWrite() {
            return raw._onWrite();
        }

        @Override
        public ValueReader<?> _reader() {
            return raw._reader();
        }

        @Override
        public String _property() {
            return raw._property();
        }

        @Override
        public String _name() {
            return raw._name();
        }

        @Override
        public String _alias() {
            return raw._alias();
        }

        @Override
        public Field<JsonObject> _alias(String alias) {
            return new HistoryProxy(raw._alias(alias));
        }

        @Override
        public @Nullable Model<?> _owner() {
            return raw._owner();
        }

        @Override
        public void _render(Renderer renderer, Writer w) {
            w.expr(this);
            renderer.virtualHistory(w, raw);
        }
    }

    @Nullable Function<@Nullable Object, @Nullable Object> _onWrite();

    @Override
    default void _render(Renderer renderer, Writer w) {
        w.expr(this);
        if (_owner() == null) {
            if (this instanceof AggregatedField ag) {
                renderer.aggregated(w, ag);
            } else {
                renderer.virtualField(w, this);
            }
        } else {
            renderer.field(w, this);
        }
    }

    ValueReader<?> _reader();

    String _property();

    String _name();

    String _alias();

    Field<T> _alias(String alias);

    @Nullable Model<?> _owner();

    @Accessors(fluent = true)
    sealed abstract class BaseField<T extends @Nullable Object, F extends BaseField<T, F>> implements Field<T> {
        @Getter
        protected final String _name;
        @Getter
        protected final String _property;
        @Getter
        protected final Model<?> _owner;
        @Getter
        protected final ValueReader<?> _reader;
        @Getter
        @Nullable
        protected final Function<@Nullable Object, @Nullable Object> _onWrite;

        @Getter
        @Nullable
        protected String _alias;


        protected BaseField(String name, @Nullable String property, Model<?> owner, ValueReader<T> reader) {
            this._name = name;
            this._property = property == null || property.isBlank() ? name : property;
            this._owner = owner;
            this._reader = reader;
            this._onWrite = null;

        }

        @SuppressWarnings("unchecked")
        protected BaseField(String name, @Nullable String property, Model<?> owner, ValueReader<T> reader, Interceptor<T> interceptor) {
            this._name = name;
            this._property = property == null || property.isBlank() ? name : property;
            this._owner = owner;
            this._reader = reader.map(v -> interceptor.intercept(true, v));
            this._onWrite = a -> interceptor.intercept(false, (T) a);

        }

        @SuppressWarnings("unchecked")
        protected <R> BaseField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<T, R> onWrite, Function<R, T> onRead) {
            this._name = name;
            this._property = property;
            this._owner = owner;
            this._reader = reader.map(onRead);
            this._onWrite = (Function<Object, Object>) onWrite;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        protected BaseField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function onWrite, @Nullable String alias) {
            this._name = name;
            this._property = property;
            this._owner = owner;
            this._reader = reader;
            this._onWrite = (Function<Object, Object>) onWrite;
            this._alias = alias;
        }

        protected abstract F ctor(String alias);

        public F _alias(String alias) {
            return ctor(alias);
        }

        public StmtSet<T> set(T v) {
            return new StmtSet<>(this, v);
        }

        public StmtSet<T> set(Field<T> v) {
            return new StmtSet<>(this, v);
        }

        public StmtSet<T> setNull() {
            return new StmtSet<>(this, null);
        }


        public StmtAssign value(T v) {
            return new StmtAssign(this, v);
        }

        public StmtAssign value(Field<T> v) {
            return new StmtAssign(this, v);
        }

        public StmtAssign valueNull() {
            return new StmtAssign(this, null);
        }
    }

    non-sealed class BooleanField extends BaseField<Boolean, BooleanField> implements Value.BooleanValue {
        @Override
        protected BooleanField ctor(String alias) {
            return new BooleanField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected BooleanField(String name, String property, Model<?> owner, ValueReader<?> reader,
                               @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public BooleanField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getBoolean);
        }

        public <R> BooleanField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<Boolean, R> onWrite, Function<R, Boolean> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public BooleanField(String name, String property, Model<?> owner, Interceptor<Boolean> interceptor) {
            super(name, property, owner, Row::getBoolean, interceptor);
        }

        public StmtSet<Boolean> set(boolean v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(boolean v) {
            return new StmtAssign(this, v);
        }
    }

    @SneakyThrows
    static @Nullable Class<?> clazz(Row r, int i) {
        var s = r.getString(i);
        if (s == null || s.isBlank()) return null;
        return Class.forName(s);
    }

    static @Nullable Duration duration(Row r, int i) {
        return Codec.duration(r.getString(i));
    }

    static @Nullable Period period(Row r, int i) {
        return Codec.period(r.getString(i));
    }

    non-sealed class StringField extends BaseField<String, StringField> implements Value.StringValue {
        public StringField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getString);
        }

        @Override
        protected StringField ctor(String alias) {
            return new StringField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected StringField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public <R> StringField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<String, R> onWrite, Function<R, String> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StringField(String name, String property, Model<?> owner,
                           Interceptor<String> interceptor) {
            super(name, property, owner, Row::getString, interceptor);
        }

    }

    non-sealed class ClassField extends BaseField<Class<?>, ClassField> implements Value.ObjectValue<Class<?>> {
        public ClassField(String name, String property, Model<?> owner) {
            super(name, property, owner, Field::clazz);
        }

        @Override
        protected ClassField ctor(String alias) {
            return new ClassField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected ClassField(String name, String property, Model<?> owner, ValueReader<?> reader,
                             @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public <R> ClassField(String name, String property, Model<?> owner, ValueReader<R> reader,
                              Function<Class<?>, R> onWrite, Function<R, Class<?>> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public ClassField(String name, String property, Model<?> owner,
                          Interceptor<Class<?>> interceptor) {
            super(name, property, owner, Field::clazz, interceptor);
        }

        @Override
        public StmtSet<Class<?>> set(@Nullable Class<?> v) {
            return new StmtSet<>(this, v == null ? null : v.getCanonicalName());
        }

        @Override
        public StmtAssign value(@Nullable Class<?> v) {
            return new StmtAssign(this, v == null ? null : v.getCanonicalName());
        }

    }

    non-sealed class DurationField extends BaseField<Duration, DurationField> implements Value.ObjectValue<Duration> {
        public DurationField(String name, String property, Model<?> owner) {
            super(name, property, owner, Field::duration);
        }

        @Override
        protected DurationField ctor(String alias) {
            return new DurationField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DurationField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public <R> DurationField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                 Function<Duration, R> onWrite, Function<R, Duration> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public DurationField(String name, String property, Model<?> owner,
                             Interceptor<Duration> interceptor) {
            super(name, property, owner, Field::duration, interceptor);
        }

        @Override
        public StmtSet<Duration> set(@Nullable Duration v) {
            return new StmtSet<>(this, v == null ? null : Codec.duration(v));
        }

        @Override
        public StmtAssign value(@Nullable Duration v) {
            return new StmtAssign(this, v == null ? null : Codec.duration(v));
        }

    }

    non-sealed class PeriodField extends BaseField<Period, PeriodField> implements Value.ObjectValue<Period> {
        public PeriodField(String name, String property, Model<?> owner) {
            super(name, property, owner, Field::period);
        }

        @Override
        protected PeriodField ctor(String alias) {
            return new PeriodField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected PeriodField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public <R> PeriodField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<Period, R> onWrite, Function<R, Period> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public PeriodField(String name, String property, Model<?> owner,
                           Interceptor<Period> interceptor) {
            super(name, property, owner, Field::period, interceptor);
        }

        @Override
        public StmtSet<Period> set(@Nullable Period v) {
            return new StmtSet<>(this, v == null ? null : Codec.period(v));
        }

        @Override
        public StmtAssign value(@Nullable Period v) {
            return new StmtAssign(this, v == null ? null : Codec.period(v));
        }

    }

    non-sealed class UUIDField extends BaseField<UUID, UUIDField> implements Value.UUIDValue {
        public UUIDField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getUUID);
        }

        @Override
        protected UUIDField ctor(String alias) {
            return new UUIDField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected UUIDField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public <R> UUIDField(String name, String property, Model<?> owner, ValueReader<R> reader,
                             Function<UUID, R> onWrite, Function<R, UUID> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public UUIDField(String name, String property, Model<?> owner,
                         Interceptor<UUID> interceptor) {
            super(name, property, owner, Row::getUUID, interceptor);
        }

        @Override
        public StmtSet<UUID> set(@Nullable UUID v) {
            return new StmtSet<>(this, v);
        }

        @Override
        public StmtAssign value(@Nullable UUID v) {
            return new StmtAssign(this, v);
        }

    }

    non-sealed class IntegerField extends BaseField<Integer, IntegerField> implements IntegerValue {
        @Override
        protected IntegerField ctor(String alias) {
            return new IntegerField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected IntegerField(String name, String property, Model<?> owner, ValueReader<?> reader,
                               @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public IntegerField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getInteger);
        }

        public <R> IntegerField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<Integer, R> onWrite, Function<R, Integer> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public IntegerField(String name, String property, Model<?> owner,
                            Interceptor<Integer> interceptor) {
            super(name, property, owner, Row::getInteger, interceptor);
        }


        public StmtSet<Integer> set(int v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(int v) {
            return new StmtAssign(this, v);
        }
    }

    non-sealed class NumericField extends BaseField<Numeric, NumericField> implements NumericValue {
        @Override
        protected NumericField ctor(String alias) {
            return new NumericField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected NumericField(String name, String property, Model<?> owner, ValueReader<?> reader,
                               @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public NumericField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getNumeric);
        }

        public <R> NumericField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<Numeric, R> onWrite, Function<R, Numeric> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public NumericField(String name, String property, Model<?> owner, Interceptor<Numeric> interceptor) {
            super(name, property, owner, Row::getNumeric, interceptor);
        }
    }

    non-sealed class DecimalField extends BaseField<BigDecimal, DecimalField> implements DecimalValue {
        @Override
        protected DecimalField ctor(String alias) {
            return new DecimalField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DecimalField(String name, String property, Model<?> owner, ValueReader<?> reader,
                               @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public DecimalField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getBigDecimal);
        }

        public DecimalField(String name, String property, Model<?> owner, Interceptor<BigDecimal> interceptor) {
            super(name, property, owner, Row::getBigDecimal, interceptor);
        }

        public <R> DecimalField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<BigDecimal, R> onWrite, Function<R, BigDecimal> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class ByteField extends BaseField<Byte, ByteField> implements ByteValue {
        @Override
        protected ByteField ctor(String alias) {
            return new ByteField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected ByteField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public ByteField(String name, String property, Model<?> owner) {
            super(name, property, owner, ((ValueReader<Integer>) Row::getInteger).map(nullable(Integer::byteValue)));
        }

        public ByteField(String name, String property, Model<?> owner, Interceptor<Byte> interceptor) {
            super(name, property, owner, ((ValueReader<Integer>) Row::getInteger).map(nullable(Integer::byteValue)), interceptor);
        }

        public <R> ByteField(String name, String property, Model<?> owner, ValueReader<R> reader,
                             Function<Byte, R> onWrite, Function<R, Byte> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<Byte> set(byte v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(byte v) {
            return new StmtAssign(this, v);
        }
    }

    non-sealed class ShortField extends BaseField<Short, ShortField> implements ShortValue {
        @Override
        protected ShortField ctor(String alias) {
            return new ShortField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected ShortField(String name, String property, Model<?> owner, ValueReader<?> reader,
                             @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public ShortField(String name, String property, Model<?> owner) {
            super(name, property, owner, ((ValueReader<Integer>) Row::getInteger).map(nullable(Integer::shortValue)));
        }

        public ShortField(String name, String property, Model<?> owner, Interceptor<Short> interceptor) {
            super(name, property, owner, ((ValueReader<Integer>) Row::getInteger).map(nullable(Integer::shortValue)), interceptor);
        }

        public <R> ShortField(String name, String property, Model<?> owner, ValueReader<R> reader,
                              Function<Short, R> onWrite, Function<R, Short> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<Short> set(short v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(short v) {
            return new StmtAssign(this, v);
        }
    }

    non-sealed class LongField extends BaseField<Long, LongField> implements LongValue {
        @Override
        protected LongField ctor(String alias) {
            return new LongField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected LongField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public LongField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getLong);
        }

        public LongField(String name, String property, Model<?> owner, Interceptor<Long> interceptor) {
            super(name, property, owner, Row::getLong, interceptor);
        }

        public <R> LongField(String name, String property, Model<?> owner, ValueReader<R> reader,
                             Function<Long, R> onWrite, Function<R, Long> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<Long> set(long v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(long v) {
            return new StmtAssign(this, v);
        }
    }


    non-sealed class FloatField extends BaseField<Float, FloatField> implements FloatValue {
        @Override
        protected FloatField ctor(String alias) {
            return new FloatField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected FloatField(String name, String property, Model<?> owner, ValueReader<?> reader,
                             @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public FloatField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getFloat);
        }

        public FloatField(String name, String property, Model<?> owner, Interceptor<Float> interceptor) {
            super(name, property, owner, Row::getFloat, interceptor);
        }

        public <R> FloatField(String name, String property, Model<?> owner, ValueReader<R> reader,
                              Function<Float, R> onWrite, Function<R, Float> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<Float> set(float v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(float v) {
            return new StmtAssign(this, v);
        }
    }

    non-sealed class DoubleField extends BaseField<Double, DoubleField> implements DoubleValue {
        @Override
        protected DoubleField ctor(String alias) {
            return new DoubleField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DoubleField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public DoubleField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getDouble);
        }

        public DoubleField(String name, String property, Model<?> owner, Interceptor<Double> interceptor) {
            super(name, property, owner, Row::getDouble, interceptor);
        }

        public <R> DoubleField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<Double, R> onWrite, Function<R, Double> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<Double> set(double v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(double v) {
            return new StmtAssign(this, v);
        }
    }

    non-sealed class InstantField extends BaseField<Instant, InstantField> implements InstantValue {
        @Override
        protected InstantField ctor(String alias) {
            return new InstantField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected InstantField(String name, String property, Model<?> owner, ValueReader<?> reader,
                               @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public InstantField(String name, String property, Model<?> owner) {
            super(name, property, owner, instantReader());
        }

        public InstantField(String name, String property, Model<?> owner, Interceptor<Instant> interceptor) {
            super(name, property, owner, instantReader(), interceptor);
        }

        public <R> InstantField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                Function<Instant, R> onWrite, Function<R, Instant> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class DateField extends BaseField<LocalDate, DateField> implements DateValue {
        @Override
        protected DateField ctor(String alias) {
            return new DateField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DateField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public DateField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getLocalDate);
        }

        public DateField(String name, String property, Model<?> owner, Interceptor<LocalDate> interceptor) {
            super(name, property, owner, Row::getLocalDate, interceptor);
        }

        public <R> DateField(String name, String property, Model<?> owner, ValueReader<R> reader,
                             Function<LocalDate, R> onWrite, Function<R, LocalDate> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class TimeField extends BaseField<LocalTime, TimeField> implements TimeValue {
        @Override
        protected TimeField ctor(String alias) {
            return new TimeField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected TimeField(String name, String property, Model<?> owner, ValueReader<?> reader,
                            @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public TimeField(String name, String property, Model<?> owner, Interceptor<LocalTime> interceptor) {
            super(name, property, owner, Row::getLocalTime, interceptor);
        }

        public TimeField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getLocalTime);
        }

        public <R> TimeField(String name, String property, Model<?> owner, ValueReader<R> reader,
                             Function<LocalTime, R> onWrite, Function<R, LocalTime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class DateTimeField extends BaseField<LocalDateTime, DateTimeField> implements DateTimeValue {
        @Override
        protected DateTimeField ctor(String alias) {
            return new DateTimeField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DateTimeField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public DateTimeField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getLocalDateTime);
        }

        public DateTimeField(String name, String property, Model<?> owner, Interceptor<LocalDateTime> interceptor) {
            super(name, property, owner, Row::getLocalDateTime, interceptor);
        }

        public <R> DateTimeField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                 Function<LocalDateTime, R> onWrite, Function<R, LocalDateTime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class TimeTZField extends BaseField<OffsetTime, TimeTZField> implements TimeTZValue {
        @Override
        protected TimeTZField ctor(String alias) {
            return new TimeTZField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected TimeTZField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public TimeTZField(String name, String property, Model<?> owner) {
            super(name, property, owner, offsetTimeReader());
        }

        public TimeTZField(String name, String property, Model<?> owner, Interceptor<OffsetTime> interceptor) {
            super(name, property, owner, offsetTimeReader(), interceptor);
        }

        public <R> TimeTZField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<OffsetTime, R> onWrite, Function<R, OffsetTime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class DateTimeTZField extends BaseField<OffsetDateTime, DateTimeTZField> implements DateTimeTZValue {
        @Override
        protected DateTimeTZField ctor(String alias) {
            return new DateTimeTZField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected DateTimeTZField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                  @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public DateTimeTZField(String name, String property, Model<?> owner) {
            super(name, property, owner, offsetDateTimeReader());
        }

        public DateTimeTZField(String name, String property, Model<?> owner, Interceptor<OffsetDateTime> interceptor) {
            super(name, property, owner, offsetDateTimeReader(), interceptor);
        }

        public <R> DateTimeTZField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                   Function<OffsetDateTime, R> onWrite, Function<R, OffsetDateTime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class BytesField extends BaseField<byte[], BytesField> implements BytesValue {
        protected BytesField(String name, String property, Model<?> owner, ValueReader<?> reader,
                             @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        @Override
        protected BytesField ctor(String alias) {
            return new BytesField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        public BytesField(String name, String property, Model<?> owner) {
            super(name, property, owner, ((ValueReader<Buffer>) Row::getBuffer).map(Fn.nullable((Function<Buffer, byte[]>) Buffer::getBytes)));
        }

        public BytesField(String name, String property, Model<?> owner, Interceptor<byte[]> interceptor) {
            super(name, property, owner, ((ValueReader<Buffer>) Row::getBuffer).map(Fn.nullable((Function<Buffer, byte[]>) Buffer::getBytes)), interceptor);
        }

        public <R> BytesField(String name, String property, Model<?> owner, ValueReader<R> reader,
                              Function<byte[], R> onWrite, Function<R, byte[]> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }


        public <R> BytesField(String name, String property, Model<?> owner, ValueReader<R> reader,
                              Function<byte[], R> onWrite) {
            super(name, property, owner, reader, onWrite, (String) null);
        }
    }

    non-sealed class BufferField extends BaseField<Buffer, BufferField> implements BufferValue {
        @Override
        protected BufferField ctor(String alias) {
            return new BufferField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected BufferField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public BufferField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getBuffer);
        }

        public BufferField(String name, String property, Model<?> owner, Interceptor<Buffer> interceptor) {
            super(name, property, owner, Row::getBuffer, interceptor);
        }

        public <R> BufferField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<Buffer, R> onWrite, Function<R, Buffer> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public <R> BufferField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<Buffer, R> onWrite) {
            super(name, property, owner, reader, onWrite, (String) null);
        }
    }

    non-sealed class JsonArrayField extends BaseField<JsonArray, JsonArrayField> implements JsonArrayValue {
        @Override
        protected JsonArrayField ctor(String alias) {
            return new JsonArrayField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected JsonArrayField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                 @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public JsonArrayField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getJsonArray);
        }

        public JsonArrayField(String name, String property, Model<?> owner, Interceptor<JsonArray> interceptor) {
            super(name, property, owner, Row::getJsonArray, interceptor);
        }


        public StmtJsonSet setAt(Object value, int key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }
            return new StmtJsonSet(StmtJsonSet.Operator.SET, this, p, value);
        }

        public StmtJsonSet mergeAt(Object value, int key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }

            return new StmtJsonSet(StmtJsonSet.Operator.MERGE, this, p, value);
        }

        public StmtJsonSet deleteAt(int key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }

            return new StmtJsonSet(this, p);
        }

    }

    non-sealed class JsonObjectField extends BaseField<JsonObject, JsonObjectField> implements JsonObjectValue {
        @Override
        protected JsonObjectField ctor(String alias) {
            return new JsonObjectField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected JsonObjectField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                  @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public JsonObjectField(String name, String property, Model<?> owner) {
            super(name, property, owner, Row::getJsonObject);
        }

        public JsonObjectField(String name, String property, Model<?> owner, Interceptor<JsonObject> interceptor) {
            super(name, property, owner, Row::getJsonObject, interceptor);
        }

        public StmtJsonSet setAt(Object value, String key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }

            return new StmtJsonSet(StmtJsonSet.Operator.SET, this, p, value);
        }

        public StmtJsonSet mergeAt(Object value, String key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }

            return new StmtJsonSet(StmtJsonSet.Operator.MERGE, this, p, value);
        }

        public StmtJsonSet deleteAt(String key, Object... path) {
            Object[] p;
            if (path.length == 0) {
                p = new Object[]{key};
            } else {
                p = new Object[path.length + 1];
                p[0] = key;
                System.arraycopy(path, 0, p, 1, path.length);
            }

            return new StmtJsonSet(this, p);
        }
    }

    non-sealed class EnumTextField<T extends Enum<T>> extends BaseField<T, EnumTextField<T>> implements
            EnumTextValue<T> {
        protected EnumTextField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        @Override
        protected EnumTextField<T> ctor(String alias) {
            return new EnumTextField<>(_name, _property, _owner, _reader, _onWrite, alias);
        }

        public EnumTextField(String name, String property, Class<T> type, Model<?> owner) {
            super(name, property, owner,
                    new ValueReader.Mapped<>(Row::getString, n -> Enum.valueOf(type, n)));
        }

        public EnumTextField(String name, String property, Class<T> type, Model<?> owner, Interceptor<T> interceptor) {
            super(name, property, owner,
                    new ValueReader.Mapped<>(Row::getString, n -> Enum.valueOf(type, n)), interceptor);
        }

    }

    non-sealed class EnumOrdinalField<T extends Enum<T>> extends BaseField<T, EnumOrdinalField<T>> implements
            EnumOrdinalValue<T> {
        @Override
        protected EnumOrdinalField<T> ctor(String alias) {
            return new EnumOrdinalField<>(_name, _property, _owner, _reader, _onWrite, alias);
        }

        public EnumOrdinalField(String name, String property, Class<T> type, Model<?> owner) {
            this(name, property, type.getEnumConstants(), owner);
        }

        protected EnumOrdinalField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                   @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public EnumOrdinalField(String name, String property, T[] type, Model<?> owner, Interceptor<T> interceptor) {
            super(name, property, owner,
                    new ValueReader.Mapped<>(Row::getInteger, n -> n < 0 || n >= type.length ? null : type[n]), interceptor);
        }

        public EnumOrdinalField(String name, String property, T[] type, Model<?> owner) {
            super(name, property, owner, new ValueReader.Mapped<>(Row::getInteger, n -> n == null || n < 0 || n >= type.length ? null : type[n]));
        }


    }

    non-sealed class ObjectField<T> extends BaseField<T, ObjectField<T>> implements Value<T> {
        protected ObjectField(String name, String property, Model<?> owner, ValueReader<?> reader,
                              @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        @Override
        protected ObjectField<T> ctor(String alias) {
            return new ObjectField<>(_name, _property, _owner, _reader, _onWrite, alias);
        }

        public ObjectField(String name, String property, Model<?> owner, ValueReader<T> reader) {
            super(name, property, owner, reader);
        }

        public ObjectField(String name, String property, Model<?> owner, ValueReader<T> reader, Interceptor<T> interceptor) {
            super(name, property, owner, reader, interceptor);
        }

        public <R> ObjectField(String name, String property, Model<?> owner, ValueReader<R> reader,
                               Function<T, R> onWrite, Function<R, T> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }
    }

    non-sealed class LongDatetimeField extends BaseField<ITimes.IDatetime, LongDatetimeField> implements LongDatetimeValue {
        static final ValueReader<ITimes.IDatetime> READER = (r, i) -> Optional.ofNullable(r.getLong(i)).map(ITimes.IDatetime::new).orElse(null);

        @Override
        protected LongDatetimeField ctor(String alias) {
            return new LongDatetimeField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected LongDatetimeField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                    @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public LongDatetimeField(String name, String property, Model<?> owner) {
            super(name, property, owner, READER);
        }

        public LongDatetimeField(String name, String property, Model<?> owner, Interceptor<ITimes.IDatetime> interceptor) {
            super(name, property, owner, READER, interceptor);
        }

        public <R> LongDatetimeField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                     Function<ITimes.IDatetime, R> onWrite, Function<R, ITimes.IDatetime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<ITimes.IDatetime> set(long v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(long v) {
            return new StmtAssign(this, v);
        }

        public StmtSet<ITimes.IDatetime> set(ITimes.@Nullable IDatetime v) {
            return new StmtSet<>(this, v == null ? null : v.value());
        }

        public StmtAssign value(ITimes.@Nullable IDatetime v) {
            return new StmtAssign(this, v == null ? null : v.value());
        }
    }

    non-sealed class IntegerDateField extends BaseField<ITimes.IDate, IntegerDateField> implements IntegerDateValue {
        static final ValueReader<ITimes.IDate> READER = new ValueReader.Mapped<>(
                Row::getInteger,
                ITimes.IDate::new
        );

        @Override
        protected IntegerDateField ctor(String alias) {
            return new IntegerDateField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected IntegerDateField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                   @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public IntegerDateField(String name, String property, Model<?> owner) {
            super(name, property, owner, READER);
        }

        public IntegerDateField(String name, String property, Model<?> owner, Interceptor<ITimes.IDate> interceptor) {
            super(name, property, owner, READER, interceptor);
        }

        public <R> IntegerDateField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                    Function<ITimes.IDate, R> onWrite, Function<R, ITimes.IDate> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<ITimes.IDate> set(int v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(int v) {
            return new StmtAssign(this, v);
        }

        public StmtSet<ITimes.IDate> set(ITimes.@Nullable IDate v) {
            return new StmtSet<>(this, v == null ? null : v.value());
        }

        public StmtAssign value(ITimes.@Nullable IDate v) {
            return new StmtAssign(this, v == null ? null : v.value());
        }
    }

    non-sealed class IntegerTimeField extends BaseField<ITimes.ITime, IntegerTimeField> implements IntegerTimeValue {
        static final ValueReader<ITimes.ITime> READER = new ValueReader.Mapped<>(
                Row::getInteger,
                ITimes.ITime::new
        );

        @Override
        protected IntegerTimeField ctor(String alias) {
            return new IntegerTimeField(_name, _property, _owner, _reader, _onWrite, alias);
        }

        protected IntegerTimeField(String name, String property, Model<?> owner, ValueReader<?> reader,
                                   @Nullable Function<Object, Object> onWrite, String alias) {
            super(name, property, owner, reader, onWrite, alias);
        }

        public IntegerTimeField(String name, String property, Model<?> owner) {
            super(name, property, owner, READER);
        }

        public IntegerTimeField(String name, String property, Model<?> owner, Interceptor<ITimes.ITime> interceptor) {
            super(name, property, owner, READER, interceptor);
        }

        public <R> IntegerTimeField(String name, String property, Model<?> owner, ValueReader<R> reader,
                                    Function<ITimes.@Nullable ITime, R> onWrite, Function<R, ITimes.@Nullable ITime> onRead) {
            super(name, property, owner, reader, onWrite, onRead);
        }

        public StmtSet<ITimes.ITime> set(int v) {
            return new StmtSet<>(this, v);
        }

        public StmtAssign value(int v) {
            return new StmtAssign(this, v);
        }

        public StmtSet<ITimes.ITime> set(ITimes.@Nullable ITime v) {
            return new StmtSet<>(this, v == null ? null : v.value());
        }

        public StmtAssign value(ITimes.@Nullable ITime v) {
            return new StmtAssign(this, v == null ? null : v.value());
        }
    }

    AtomicBoolean SUPPORT_OFFSET_TIME = new AtomicBoolean(false);

    static ValueReader<OffsetDateTime> offsetDateTimeReader() {
        return SUPPORT_OFFSET_TIME.get()
                ? Row::getOffsetDateTime
                : ((r, i) -> r.getLocalDateTime(i).atOffset(OFFSET_TIMEZONE.get()));
    }

    static ValueReader<OffsetTime> offsetTimeReader() {
        return SUPPORT_OFFSET_TIME.get()
                ? Row::getOffsetTime
                : ((r, i) -> r.getLocalTime(i).atOffset(OFFSET_TIMEZONE.get()));
    }

    /// datasource use OFFSET_TIME to present timestamp
    AtomicBoolean INSTANT_OFFSET_MODE = new AtomicBoolean(false);
    /// default data source timestamp offset
    AtomicReference<ZoneOffset> OFFSET_TIMEZONE = new AtomicReference<>(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    /// default data source timestamp offset
    AtomicReference<ZoneOffset> OFFSET_INSTANT = new AtomicReference<>(ZoneOffset.UTC);

    static ValueReader<Instant> instantReader() {
        return (INSTANT_OFFSET_MODE.get())
                ? ((ValueReader<OffsetDateTime>) Row::getOffsetDateTime).map(nullable(OffsetDateTime::toInstant))
                : ((ValueReader<LocalDateTime>) Row::getLocalDateTime).map(nullable(Field::from));
    }

    static Instant from(LocalDateTime ldt) {
        return ldt.toInstant(OFFSET_INSTANT.get());
    }

    enum Aggregated {
        MAX, MIN, AVG, SUM, COUNT
    }

    record AggregatedField(
            Aggregated mode,
            Field<?> source,
            AtomicReference<@Nullable String> aName) implements Field<Numeric>, NumericValue {
        @Override
        public ValueReader<Numeric> _reader() {
            return Row::getNumeric;
        }

        @Override
        public @Nullable Function<Object, Object> _onWrite() {
            return null;
        }

        @Override
        public String _property() {
            return Optional.ofNullable(aName.get()).orElseGet(source::_property);
        }

        @Override
        public String _name() {
            return Objects.requireNonNull(aName.get());
        }

        @Override
        public String _alias() {
            return Objects.requireNonNull(aName.get());
        }

        @Override
        public Field<Numeric> _alias(String alias) {
            aName.set(alias);
            return this;
        }

        @Override
        public @Nullable Model<?> _owner() {
            return null;
        }
    }
}
