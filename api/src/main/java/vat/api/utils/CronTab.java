package vat.api.utils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vat.api.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

///
/// @author Zen.Liu
/// @since 2025-11-19


@SuppressWarnings("unused")
public interface CronTab extends Data {
    Logger log = LoggerFactory.getLogger(CronTab.class);

    /// have seconds perceptions.
    boolean seconds();

    /// last execute tick (-1 for none)
    long tick();

    /// set last execute tick (-1 for none)
    CronTab tick(long t);

    /// check data is valid
    boolean validate();

    /// same as check but will not store the tick.
    boolean checkOnly(LocalDateTime datetime);

    default boolean checkOnly(long datetime) {
        return checkOnly(ITimes.datetime(datetime));
    }

    /// check if match execute schedule. when match will save this tick as last execute tick.
    boolean check(LocalDateTime datetime);

    default boolean check(long datetime) {
        return check(ITimes.datetime(datetime));
    }

    @SuppressWarnings("LoggingSimilarMessage")
    final class cron implements CronTab {
        final int[] p;
        ///  when dynamic never needs to calculate tick
        public final boolean dynamic;
        private volatile long tick = -1;

        public List<Tuple4<Byte, Byte, Byte, Byte>> dump() {
            return IntStream.of(p)
                    .mapToObj(Field.EXTRACTOR).toList();
        }

        public List<String> dumpText() {
            return IntStream.of(p)
                    .mapToObj(Field.FORMATTER).toList();
        }

        public cron(JsonObject j) {
            this(Objects.requireNonNull(j.getString("pattern"), "missing pattern"), j.getLong("tick", -1L));
        }

        public cron(JsonObject j, Void unused) {
            this(Objects.requireNonNull(j.getString("pattern"), "missing pattern"), j.getString("tick", "-1"));
        }

        public cron(int[] p) {
            assert p.length == 5 || p.length == 6 : "invalid pattern";
            this.p = p;
            this.dynamic = checkDynamic(p);
        }

        static boolean checkDynamic(int[] p) {
            return IntStream.of(p)
                    .anyMatch(i -> Field.FLAG.apply(i) != Field.ANY);
        }

        public cron(int[] p, long tick) {
            assert p.length == 5 || p.length == 6 : "invalid pattern";
            this.p = p;
            this.dynamic = checkDynamic(p);
            this.tick = tick;
        }

        public cron(String p) {
            this(Field.parseValidate(p));
        }

        public cron(String p, long tick) {
            this(Field.parseValidate(p), tick);
        }

        public cron(String p, String tick) {
            this(Field.parseValidate(p), Long.parseLong(tick));
        }

        @Override
        public JsonObject toJS() {
            return JsonObject.of(
                    "pattern", Field.format(p),
                    "tick", String.valueOf(tick)
            );
        }

        @Override
        public JsonObject toJson() {
            return JsonObject.of(
                    "pattern", Field.format(p),
                    "tick", tick
            );
        }

        @Override
        public JsonObject asJson() {
            return toJson();
        }

        @Override
        public boolean seconds() {
            return p.length == 6;
        }

        @Override
        public long tick() {
            return this.tick;
        }

        @Override
        public cron tick(long t) {
            this.tick = t;
            return this;
        }

        @Override
        public boolean validate() {
            return Field.validateFields(p);
        }

        @Override
        public boolean checkOnly(LocalDateTime datetime) {
            if (dynamic) {
                for (int i = p.length - 1; i >= 0; i--) {
                    if (!Field.CheckFieldMatch.test(p[i], datetime, tick)) {
                        if (log.isTraceEnabled())
                            log.warn("cron check failed for {}({}) {} {} ", i, Field.FORMATTER.apply(p[i]), this,
                                    datetime);
                        return false;
                    }
                }
            } else {
                for (int i = p.length - 1; i >= 0; i--) {
                    if (!Field.CheckFieldMatchFixPoint.test(p[i], datetime, tick)) {
                        if (log.isTraceEnabled())
                            log.warn("cron check failed for {}({}) {} {}", i, Field.FORMATTER.apply(p[i]), this,
                                    datetime);
                        return false;
                    }
                }
            }
            if (log.isTraceEnabled()) log.warn("cron success for {} {} {}", this, datetime, Tick.FORMATTER.apply(tick));
            return true;
        }


        @Override
        public boolean check(LocalDateTime datetime) {
            var c = checkOnly(datetime);
            if (c) tick = Tick.ToTick.apply(datetime);
            return c;
        }

        @Override
        public String toString() {
            return "\"" + Field.format(p) + "\"<" + (tick == -1 ? "null" : Tick.FORMATTER.apply(tick)) + ">";
        }
    }

    interface Task {
        /// the cron assigned with task
        CronTab cron();

        /// update the cron
        Task cron(CronTab cron);

        boolean checkOnly(LocalDateTime d);

        /// check and execute the task
        Future<Void> run(LocalDateTime d);

        @EqualsAndHashCode
        final class task implements Task {
            private volatile CronTab cron;
            private final Function<LocalDateTime, Future<Void>> action;

            @Override
            public boolean checkOnly(LocalDateTime d) {
                return cron.checkOnly(d);
            }

            public task(CronTab cron, Function<LocalDateTime, Future<Void>> action) {
                this.cron = cron;
                this.action = action;
            }

            @Override
            public CronTab cron() {
                return cron;
            }

            @Override
            public Task cron(CronTab cron) {
                this.cron = cron;
                return this;
            }

            @Override
            public Future<Void> run(LocalDateTime d) {
                return cron.check(d) ? action.apply(d) : Future.succeededFuture();
            }
        }
    }

    interface Scheduler {
        List<? extends Task> tasks();

        Scheduler register(Task t);

        Scheduler remove(Task t);

        Scheduler clear();

        /// on tick
        ///
        /// @param batch execute batch or sequenced
        Future<Void> tick(boolean batch);

        record scheduler(LinkedList<Task> tasks) implements Scheduler {

            @Override
            public Scheduler register(Task t) {
                tasks.add(t);
                return this;
            }

            @Override
            public Scheduler remove(Task t) {
                tasks.remove(t);
                return this;
            }

            @Override
            public Scheduler clear() {
                tasks.clear();
                return this;
            }

            @Override
            public Future<Void> tick(boolean batch) {
                if (tasks.isEmpty()) return Future.succeededFuture();
                var d = LocalDateTime.now(ITimes.ZONE.get());
                if (batch) {
                    var s = tasks.stream()
                            .filter(x -> x.checkOnly(d))
                            .distinct()
                            .map(x -> x.run(d))
                            .toList();
                    return s.isEmpty() ? Future.succeededFuture()
                            : Future.all(s).mapEmpty();
                } else {
                    var s = tasks
                            .stream()
                            .filter(x -> x.checkOnly(d)).distinct()
                            .<Supplier<Future<Void>>>map(x -> () -> x.run(d))
                            .toList();
                    return s.isEmpty() ? Future.succeededFuture()
                            : Fn.Many.Flat.seqSupplier(s);
                }

            }
        }
    }

    static Task task(String pattern, LongFunction<Future<Void>> action) {
        return new Task.task(new cron(pattern), d -> action.apply(ITimes.datetime(d)));
    }

    static Task task(String pattern, Function<LocalDateTime, Future<Void>> action) {
        return new Task.task(new cron(pattern), action);
    }

    static Task task(String pattern, Supplier<Future<Void>> action) {
        return new Task.task(new cron(pattern), i -> action.get());
    }

    static Task task(CronTab cron, Supplier<Future<Void>> action) {
        return new Task.task(cron, i -> action.get());
    }

    static Task task(CronTab cron, LongFunction<Future<Void>> action) {
        return new Task.task(cron, i -> action.apply(ITimes.datetime(i)));
    }

    static Task task(CronTab cron, Function<LocalDateTime, Future<Void>> action) {
        return new Task.task(cron, action);
    }

    /// create a scheduler
    static Scheduler scheduler() {
        return new Scheduler.scheduler(new LinkedList<>());
    }

    static CronTab cron(String pattern) {
        return new cron(pattern);
    }

    static CronTab cron(String pattern, long tick) {
        return new cron(pattern, tick);
    }

    static CronTab cron(String pattern, LocalDateTime tick) {
        return new cron(pattern, Tick.ToTick.apply(tick));
    }

    /// every first minute of hour
    CronTab HOURLY = cron("0 * * * *");
    /// every midnight of day
    CronTab DAILY = cron("0 0 * * *");
    /// every midnight of sunday
    CronTab WEEKLY = cron("0 0 * * 7");
    /// every midnight of 1st date
    CronTab MONTHLY = cron("0 0 1 * *");

    CronTab EVERY_MINUTE = cron("*/1 * * * *");

    interface Field {

        byte FIELD_SECOND = 0;
        byte FIELD_MINUTE = 1;
        byte FIELD_HOUR = 2;
        byte FIELD_DAY = 3;
        byte FIELD_MONTH = 4;
        byte FIELD_WEEKDAY = 5;
        IntFunction<String> FIELD_NAME = f -> switch (f) {
            case Field.FIELD_SECOND -> "SECOND";
            case Field.FIELD_MINUTE -> "MINUTE";
            case Field.FIELD_HOUR -> "HOUR";
            case Field.FIELD_DAY -> "DAY";
            case Field.FIELD_MONTH -> "MONTH";
            case Field.FIELD_WEEKDAY -> "WEEKDAY";
            default -> "UNK";
        };
        ///  ANY or fixed value
        byte ANY = 0;
        /// value in range
        byte RANG = 1;
        /// value differ
        byte EVERY = 2;
        IntFunction<String> FLAG_NAME = f -> switch (f) {
            case Field.ANY -> "any";
            case Field.RANG -> "range";
            case Field.EVERY -> "every";
            default -> "UNK";
        };
        byte VALUE_INVALID = 127;
        IntFunction<String> VALUE_NAME = f -> f == Field.VALUE_INVALID ? "any" : "" + f;

        getFieldFunc FIELD = i -> (byte) (i & 0xF);
        getFieldFunc FLAG = i -> (byte) ((i >> 4) & 0xF);
        getFieldFunc V0 = i -> (byte) ((i >> 8) & 0xFF);
        getFieldFunc V1 = i -> (byte) ((i >> 16) & 0xFF);

        setFieldFunc SetFIELD = (i, v) -> (i | (v & 0xF));
        setFieldFunc SetFLAG = (i, v) -> (i | ((v & 0xF) << 4));
        setFieldFunc SetV0 = (i, v) -> (i | (v << 8));
        setFieldFunc SetV1 = (i, v) -> (i | (v << 16));
        checkFieldFunc CheckField = i -> {
            var every = FLAG.apply(i) == EVERY;
            return switch (FIELD.apply(i)) {
                case FIELD_SECOND, FIELD_MINUTE -> (every && every(V0.apply(i))) ||
                                                   (range(V0.apply(i), 0, 59) && range(V1.apply(i), 0, 50));
                case FIELD_HOUR -> (every && every(V0.apply(i))) ||
                                   (range(V0.apply(i), 0, 23)
                                    && range(V1.apply(i), 0, 23));
                case FIELD_DAY -> (every && every(V0.apply(i))) ||
                                  (range(V0.apply(i), 1, 31)
                                   && range(V1.apply(i), 1, 31));
                case FIELD_MONTH -> (every && every(V0.apply(i))) || (
                        range(V0.apply(i), 1, 12)
                        && range(V1.apply(i), 1, 12)
                );
                case FIELD_WEEKDAY -> !every
                                      && range(V0.apply(i), 1, 7)
                                      && range(V1.apply(i), 1, 7)
                ;
                default -> throw new IllegalArgumentException("invalid pattern value " + i);
            };
        };

        static boolean every(byte v) {
            return v > 0 && v < VALUE_INVALID;
        }

        IntFunction<Tuple4<Byte, Byte, Byte, Byte>> EXTRACTOR = x -> Tuple.tuple(
                Field.FIELD.apply(x)
                , Field.FLAG.apply(x)
                , Field.V0.apply(x)
                , Field.V1.apply(x)
        );
        IntFunction<String> FORMATTER = i -> {
            var f = FIELD.apply(i);
            var a = FLAG.apply(i);
            var v0 = V0.apply(i);
            var v1 = V1.apply(i);
            return
                    "(%s,%s,%s,%s)".formatted(
                            FIELD_NAME.apply(f)
                            , FLAG_NAME.apply(a)
                            , VALUE_NAME.apply(v0)
                            , VALUE_NAME.apply(v1)
                    );
        };
        /// (field,current,last_execute)->can execute
        checkFieldMatch CheckFieldMatch = (i, v, t) -> switch (FIELD.apply(i)) {
            case FIELD_SECOND -> match(i, v.getSecond(), t == -1 ? -1 : Tick.SECOND.apply(t));
            case FIELD_MINUTE -> match(i, v.getMinute(), t == -1 ? -1 : Tick.MINUTE.apply(t));
            case FIELD_HOUR -> match(i, v.getHour(), t == -1 ? -1 : Tick.HOUR.apply(t));
            case FIELD_DAY -> match(i, v.getDayOfMonth(), t == -1 ? -1 : Tick.DAY.apply(t));
            case FIELD_MONTH -> match(i, v.getMonthValue(), t == -1 ? -1 : Tick.MONTH.apply(t));
            case FIELD_WEEKDAY -> match(i, v.getDayOfWeek().getValue(), t == -1 ? -1 : Tick.WEEKDAY.apply(t));
            default -> throw new IllegalArgumentException("invalid pattern value " + i);
        };
        /// (field,current,unused)->can execute
        checkFieldMatch CheckFieldMatchFixPoint = (i, v, unused) -> {
            assert FLAG.apply(i) == ANY;
            var v0 = V0.apply(i);
            return switch (FIELD.apply(i)) {
                case FIELD_SECOND -> v0 == VALUE_INVALID || v0 == v.getSecond();
                case FIELD_MINUTE -> v0 == VALUE_INVALID || v0 == v.getMinute();
                case FIELD_HOUR -> v0 == VALUE_INVALID || v0 == v.getHour();
                case FIELD_DAY -> v0 == VALUE_INVALID || v0 == v.getDayOfMonth();
                case FIELD_MONTH -> v0 == VALUE_INVALID || v0 == v.getMonthValue();
                case FIELD_WEEKDAY -> v0 == VALUE_INVALID || v0 == v.getDayOfWeek().getValue();
                default -> throw new IllegalArgumentException("invalid pattern value " + i);
            };
        };
        makeFieldFunc MakeField = (t, f, v0, v1) ->
                SetFIELD.apply(
                        SetFLAG.apply(
                                SetV0.apply(
                                        SetV1.apply(0, v1)
                                        , v0), f), t);

        static boolean range(byte v, int min, int max) {
            return v == VALUE_INVALID || (v >= min && v <= max);
        }

        static boolean match(int v, @Range(from = 0, to = 59) int value, @Range(from = -1, to = 59) short tick) {
            var v0 = V0.apply(v);
            return switch (FLAG.apply(v)) {
                case ANY -> tick == -1 || v0 == VALUE_INVALID || v0 == value;
                case RANG -> tick == -1 || (value <= v0 && value >= V1.apply(v));
                case EVERY -> tick == -1 || (v0 == differ(FIELD.apply(v), value, tick));
                default -> throw new IllegalArgumentException("invalid flag of " + v);
            };
        }

        static int differ(byte field, int value, short tick) {
            var v = switch (field) {
                case FIELD_MINUTE, FIELD_SECOND -> tick > value ? value + 60 - tick : value - tick;
                case FIELD_HOUR -> tick > value ? value + 24 - tick : value - tick;
                case FIELD_MONTH -> tick > value ? value + 12 - tick : value - tick;
                case FIELD_WEEKDAY -> tick > value ? value + 7 - tick : value - tick;
                default -> tick > value ? tick - value : value - tick;
            };
            if (log.isTraceEnabled())
                log.warn("differ for {} and {} of {} is {}", value, tick, FIELD_NAME.apply(field), v);
            return v;
        }

        interface getFieldFunc {
            byte apply(int v);
        }

        interface checkFieldFunc {
            boolean test(int v);
        }

        interface checkFieldMatch {
            boolean test(int v, LocalDateTime d, long tick);
        }

        interface setFieldFunc {
            int apply(int v, int x);
        }

        interface makeFieldFunc {
            int apply(int type, int flag, int v0, int v1);
        }
        private static IllegalArgumentException error(char c, String v, int i) {
            return new IllegalArgumentException("invalid pattern '%s' of \"%s\"[%d]".formatted(c, v, i));
        }
        static int[] parse(String pattern) {
            var v = pattern.trim();
            var n = new int[6];
            int f = -1, p = 0, x = VALUE_INVALID, y = VALUE_INVALID;
            for (var i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (p > 5) throw new IllegalArgumentException("overflow %s at %d".formatted(c, i));
                switch (c) {
                    case '*' -> {
                        if (f != -1) throw error(c, v, i);
                        f = ANY;
                    }
                    case '/' -> {
                        if (f == ANY && x == VALUE_INVALID) f = EVERY;
                        else throw error(c, v, i);
                    }
                    case '-' -> {
                        if (f == ANY && x != VALUE_INVALID) {
                            f = RANG; y = x; x = VALUE_INVALID;
                        } else throw error(c, v, i);
                    }
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        x = (x != VALUE_INVALID) ? x * 10 + (c - '0') : (c - '0');
                        if (f == -1) f = ANY;
                    }
                    case ' ', '\t' -> {
                        if (x == VALUE_INVALID && f == -1) continue;
                        n[p++] = MakeField.apply(0, f, Math.min(x, y == VALUE_INVALID ? x : y), Math.max(x, y));
                        x = VALUE_INVALID; y = VALUE_INVALID; f = -1;
                    }
                    default -> throw error(c, v, i);
                }
            }
            // Process the final segment
            n[p] = MakeField.apply(0, f, x, y);
            // Dynamic field indexing
            var hasSeconds = (p > 4);
            var s = hasSeconds ? FIELD_SECOND : FIELD_MINUTE;
            for (var i = 0; i <= p; i++) {
                n[i] = SetFIELD.apply(n[i], s + i);
            }
            return hasSeconds ? n : Arrays.copyOf(n, 5);
            /*
            var n = new int[6];//! field 0-6
            Arrays.fill(n, 0);
            var f = -1; //! flag
            var p = 0;//! pointer
            var v = pattern.trim();
            var x = VALUE_INVALID;
            var y = VALUE_INVALID;
            for (var i = 0; i < v.length(); i++) {
                var c = v.charAt(i);
                if (p > 5) throw new IllegalArgumentException("overflow %s of \"%s\"[%d]".formatted(c, v, i));
                switch (c) {
                    case '*' -> {
                        if (f == -1) {
                            f = ANY;
                            continue;
                        }
                        throw new IllegalArgumentException("invalid pattern '%s' of \"%s\"[%d]".formatted(c, v, i));
                    }
                    case '/' -> {
                        if (f == ANY && x == VALUE_INVALID) {
                            f = EVERY;
                            continue;
                        }
                        throw new IllegalArgumentException("invalid pattern '%s' of \"%s\"[%d]".formatted(c, v, i));
                    }
                    case '-' -> {
                        if (f == ANY && x != VALUE_INVALID) {
                            f = RANG;
                            y = x;
                            x = VALUE_INVALID;
                            continue;
                        }
                        throw new IllegalArgumentException("invalid pattern '%s' of \"%s\"[%d]".formatted(c, v, i));
                    }
                    default -> {
                        if (c >= '0' && c <= '9') {
                            x = (byte) (x != VALUE_INVALID ? x * 10 + (c - '0') : (c - '0'));
                            if (f == -1) f = ANY;
                            continue;
                        } else if (c == ' ' || c == '\t') {
                            if (x == VALUE_INVALID && f == -1)
                                continue;
                            else
                                n[p] = MakeField.apply(0, f, x > y ? y : x, x > y ? x : y);

                            p++;
                            x = VALUE_INVALID;
                            y = VALUE_INVALID;
                            f = -1;
                            continue;
                        }
                        throw new IllegalArgumentException("invalid pattern '%s' of \"%s\"[%d]".formatted(c, v, i));
                    }
                }
            }
            n[p] = MakeField.apply(0, f, x, y);
            if (p > 4) {
                var s = FIELD_SECOND;
                for (int i = 0; i < n.length; i++) {
                    n[i] = SetFIELD.apply(n[i], s);
                    s++;
                }
                return n;
            } else {
                var s = FIELD_MINUTE;
                for (int i = 0; i < n.length; i++) {
                    n[i] = SetFIELD.apply(n[i], s);
                    s++;
                }
                return Arrays.copyOf(n, n.length - 1);
            }*/
        }

        static boolean validateFields(int[] v) {
            for (int i : v) {
                if (!CheckField.test(i))
                    return false;
            }
            return true;
        }

        static int[] parseValidate(String pattern) {
            var v = parse(pattern);
            for (int i : v) {
                if (!CheckField.test(i))
                    throw new IllegalArgumentException("%d of %s invalid".formatted(i, Arrays.toString(v)));
            }
            return v;
        }

        static byte[] extract(int v) {
            return new byte[]{
                    FIELD.apply(v),
                    FLAG.apply(v),
                    V0.apply(v),
                    V1.apply(v),
            };
        }

        static String format(int[] v) {
            if (v.length < 5 || v.length > 6)
                throw new IllegalArgumentException("invalid pattern data " + Arrays.toString(v));
            var b = new StringBuilder();
            for (int i = 0; i < v.length; i++) {
                var x = v[i];
                var s = FLAG.apply(x);
                var v0 = V0.apply(x);
                var v1 = V1.apply(x);
                switch (s) {
                    case ANY -> {
                        if (v0 == VALUE_INVALID) b.append('*');
                        else b.append(v0);
                    }
                    case RANG -> b.append(v0).append('-').append(v1);
                    case EVERY -> b.append('*').append('/').append(v0);
                    default -> throw new IllegalArgumentException(
                            "invalid pattern data " + Arrays.toString(v) + " of " + x + " at " + i);
                }
                b.append(' ');
            }
            b.setLength(b.length() - 1);
            return b.toString();
        }

    }

    interface Tick {

        /// TICK = WEEKDAY|MONTH|DAY|HOUR|MINUTE|SECONDS (byte for each value)
        getTickFunc WEEKDAY = t -> (byte) (t & 0xFF);
        getTickFunc SECOND = t -> (byte) ((t >> 8) & 0xFF);
        getTickFunc MINUTE = t -> (byte) ((t >> 16) & 0xFF);
        getTickFunc HOUR = t -> (byte) ((t >> 24) & 0xFF);
        getTickFunc DAY = t -> (byte) ((t >> 32) & 0xFF);
        getTickFunc MONTH = t -> (byte) ((t >> 40) & 0xFF);
        getTickYearFunc YEAR = t -> (short) ((t >> 48));

        setTickFunc SetWEEKDAY = (i, t) -> i | t;
        setTickFunc SetSECOND = (i, t) -> i | ((long) (t) << 8);
        setTickFunc SetMINUTE = (i, t) -> i | ((long) (t) << 16);
        setTickFunc SetHOUR = (i, t) -> i | ((long) (t) << 24);
        setTickFunc SetDAY = (i, t) -> i | ((long) (t) << 32);
        setTickFunc SetMONTH = (i, t) -> i | ((long) (t) << 40);
        setTickFunc SetYEAR = (i, t) -> i | ((long) (t) << 48);
        LongFunction<String> FORMATTER = i -> "%02d-%02d-%02dT%02d:%02d:%02d,%d".formatted(
                YEAR.apply(i),
                MONTH.apply(i),
                DAY.apply(i),
                HOUR.apply(i),
                MINUTE.apply(i),
                SECOND.apply(i),
                WEEKDAY.apply(i)
        );
        FromTickFunc<LocalDateTime> FromTick = i ->
                LocalDateTime.of(
                        YEAR.apply(i),
                        MONTH.apply(i),
                        DAY.apply(i),
                        HOUR.apply(i),
                        MINUTE.apply(i),
                        SECOND.apply(i)
                );
        ToTickFunc<LocalDateTime> ToTick = d ->
                SetWEEKDAY.apply(
                        SetSECOND.apply(
                                SetMINUTE.apply(
                                        SetHOUR.apply(
                                                SetDAY.apply(
                                                        SetMONTH.apply(
                                                                SetYEAR.apply(0, d.getYear())
                                                                , d.getMonthValue())
                                                        , d.getDayOfMonth())
                                                , d.getHour())
                                        , d.getMinute())
                                , d.getSecond())
                        , d.getDayOfWeek().getValue());

        interface setTickFunc {
            long apply(long v, int s);

        }

        interface FromTickFunc<T> {
            T apply(long tick);
        }

        interface ToTickFunc<T> {
            long apply(T t);
        }

        interface getTickFunc {
            byte apply(long v);
        }

        interface getTickYearFunc {
            short apply(long v);
        }

    }
}
