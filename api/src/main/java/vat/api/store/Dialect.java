package vat.api.store;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import org.jetbrains.annotations.Nullable;
import vat.api.implement.Codec;

import java.time.Duration;
import java.time.Period;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public interface Dialect extends Renderer {
    //region helper
    default Reader<?> selectReader(State state) {
        if (state.picks != null && state.picks.size() == 1) {
            var x = state.picks.getFirst();
            if (x instanceof Field<?> f) {
                return f._reader().toReader(0);
            } else if (x instanceof JsonGet<?, ?> f) {
                return f.reader().toReader(0);
            }
            throw new IllegalArgumentException("Unknown reader type for " + x.getClass());
        }
        if (state.picks != null && state.picks.size() > 1) {
            return new TupleReader(state.picks.stream().map(x -> {
                if (x instanceof Field<?> f) {
                    return f._reader();
                } else if (x instanceof JsonGet<?, ?> f) {
                    return f.reader();
                }
                throw new IllegalArgumentException("Unknown reader type for " + x.getClass());
            }).toList());
        }
        if (state.joined == null || state.joined.isEmpty()) {
            return Reader.combine(state.primary._create(), state.primary._fields());
        }
        var readers = new ArrayList<TupleObjectReader.TupleValueReader<?>>();
        readers.add(TupleObjectReader.TupleValueReader.from(state.primary));
        for (var stmtJoin : state.joined) {
            readers.add(TupleObjectReader.TupleValueReader.from(stmtJoin.model()));
        }
        return new TupleObjectReader(readers);
    }

    List<Statement.SetStmt> updateAuditInjects(State state);

    void historicInject(State state, Set<Field<?>> fieldSet, List<Statement.SetStmt> extra);

    default <T extends FieldAndValue> Object writerApply(T t) {
        var w = t.left()._onWrite();
        var x = t.value();
        if (w != null && !(x instanceof Renderable))
            return w.apply(x);
        else
            return x;
    }

    default <T extends FieldAndValue> BiConsumer<Writer, T> writerJsonConsume(char sep, BiConsumer<Writer, Object> inner) {
        return (writer, t) -> {
            if (t instanceof StmtJsonSet j) {
                writer.render(j);
                return;
            }
            var w = t.left()._onWrite();
            var x = t.value();
            inner.accept(writer, t.left());
            writer.w(sep); //! TODO
            if (w != null && !(x instanceof Renderable))
                inner.accept(writer, w.apply(x));
            else
                inner.accept(writer, x);
        };
    }

    default <T extends FieldAndValue> BiConsumer<Writer, T> writerConsumeValue(BiConsumer<Writer, Object> inner) {
        return (writer, t) -> {
            var w = t.left()._onWrite();
            var x = t.value();
            if (w != null && !(x instanceof Renderable))
                inner.accept(writer, w.apply(x));
            else
                inner.accept(writer, x);
        };
    }

    List<StmtAssign> insertAuditInjects(State state);

    void insertAuditInject(State state);

    //endregion
    /// invoke after instanced, to config shared properties.
    void config();
    default String name() {
        return this.getClass().getSimpleName();
    }

    /// pre convert parameters to dialect specified parameter type
    default Object parameter(Writer w, Object value) {
        return switch (value) {
            case Duration d -> Codec.duration(d);
            case Period d -> Codec.period(d);
            default -> value;
        };
    }

    /// render a state of query to rendered;
    default Rendered render(State state, Rendered rendered) {
        switch (state.type) {
            case SELECT_ONE, SELECT_ANY -> select(rendered.query, state, rendered, state.type == QueryType.SELECT_ONE);
            case COUNTING -> count(rendered.query, state, rendered);
            case UPDATE -> update(rendered.query, state, rendered);
            case INSERT -> insert(rendered.query, state, rendered, false);
            case INSERT_RETURNS -> insert(rendered.query, state, rendered, true);
            case INSERT_MULTI -> insertMulti(rendered.query, state, rendered, false);
            case INSERT_MULTI_RETURNS -> insertMulti(rendered.query, state, rendered, true);
            case DELETE -> {
                if (state.permanent || TraitField.REMOVED.of(state.primary) == null)
                    delete(rendered.query, state, rendered);
                else softDelete(rendered.query, state, rendered);
            }
            default -> throw new IllegalStateException("Unknown state: " + state.type);
        }
        return rendered;
    }

    //region segment builder
    default void whereBuilder(Writer q, State state, Rendered rendered, boolean permanent) {
        var ps = state.primary._softRemovable();
        var jps = state.joined != null && state.joined.stream().anyMatch(x -> x.model()._softRemovable());
        if (state.cond == null && !ps && !jps) return;
        rendered.stage = Stage.WHERE;
        q.wp("WHERE");
        if (state.cond != null)
            rendered.render(q, state.cond);
        var removed = TraitField.REMOVED.of(state.primary);
        if(!permanent){
            if (removed != null) {
                q.when(state.cond != null, " AND ").use(r -> ((Value.BooleanValue) removed).isFalse()._render(rendered, r));
            }
        }
        if (state.joined != null) {
            var other = state.joined.stream().map(x -> x.model()._traits(TraitField.REMOVED))
                    .filter(Objects::nonNull)
                    .map(x -> ((Value.BooleanValue) x).isFalse())
                    .toList();
            q.when((!permanent&&removed != null) || state.cond != null, " AND ")
                    .each(other, " AND ", rendered::render);
        }
    }

    //endregion

    //region query builder
     void delete(Writer q, State state, Rendered rendered);

     void softDelete(Writer q, State state, Rendered rendered);

    void insert(Writer q, State state, Rendered rendered, boolean returning);


    void insertMulti(Writer q, State state, Rendered rendered, boolean returning);

    static Field<?>[] ignoreHistory(Model<?> primary){
        if(primary._historic()){
            var his=primary._traits(TraitField.HISTORY);
            return Arrays.stream(primary._fields()).sequential()
                    .map(x->x.equals(his)?new Field.HistoryProxy(his):x)
                    .toArray(Field<?>[]::new);
        }
        return primary._fields();
    }
    default void select(Writer q, State state, Rendered rendered, boolean singular) {
        rendered.stage = Stage.SELECT;
        q.w("SELECT").sp();
        if (state.hasPick()) {
            q.each(state.picks, ',', rendered::render);
        } else if(state.withHistory) {
            q.each(state.primary._fields(), ',', rendered::render);
            if (state.hasJoin()) {
                q.w(',').each(state.joined, StmtJoin::model, ',', (w, m) -> w.each(m._fields(), ',', rendered::render));
            }
        }else{
            q.each(ignoreHistory(state.primary), ',', rendered::render);
            if (state.hasJoin()) {
                q.w(',').each(state.joined, StmtJoin::model, ',', (w, m) -> w.each(ignoreHistory(m), ',', rendered::render));
            }
        }
        rendered.reader = selectReader(state);
        rendered.stage = Stage.FROM;
        q.wp("FROM");
        rendered.render(q, state.primary);
        if (state.joined != null && !state.joined.isEmpty()) {
            rendered.stage = Stage.JOIN;
            q.sp().each(state.joined, ' ', rendered::render);
        }
        whereBuilder(q, state, rendered,false);
        if (state.grouped != null) {
            rendered.stage = Stage.GROUP_BY;
            q.wp("GROUP BY").each(state.grouped, ',', rendered::render);
            if (state.having != null) {
                rendered.stage = Stage.HAVING;
                q.wp("HAVING");
                rendered.render(q, state.having);
            }
        }
        if (state.limit != null || state.skip != null) {
            rendered.stage = Stage.LIMITS;
            if (singular && state.limit == null)
                limits(q, state.skip, 1);
            else
                limits(q, state.skip, state.limit);
        }

    }

    default void count(Writer q, State state, Rendered rendered) {
        rendered.stage = Stage.COUNTING;
        q.w("SELECT").sp().w("COUNT(1)");
        rendered.reader = r -> r.getInteger(0);
        rendered.stage = Stage.FROM;
        q.wp("FROM");
        rendered.render(q, state.primary);
        if (state.joined != null && !state.joined.isEmpty()) {
            rendered.stage = Stage.JOIN;
            q.sp().each(state.joined, ' ', rendered::render);
        }
        whereBuilder(q, state, rendered,false);
        if (state.grouped != null) {
            rendered.stage = Stage.GROUP_BY;
            q.sp().w("GROUP BY").sp().each(state.grouped, ',', rendered::render);
            if (state.having != null) {
                rendered.stage = Stage.HAVING;
                q.sp().w("HAVING").sp();
                rendered.render(q, state.having);
            }
        }

    }

     void update(Writer q, State state, Rendered rendered);
    //endregion

    interface ReturningProcessor {
        Future<List<?>> process(SqlClient sql, SqlResult<Void> modified, Reader<?> reader);
    }

    /// method to handle return update or inserted value.
    ///
    /// @return null for query for returning is supported, otherwise should not be null.
    default @Nullable ReturningProcessor returning(State state) {
        return null;
    }

    @Deprecated(since = "should not impl")
    @Override
    default Stage stage() {
        throw new IllegalStateException("should never be called");
    }

    /// translate error to {@link StoreError}
    Throwable exceptionHandler(Throwable ex);

    default String valueNull() {
        return "null";
    }

    void limits(Writer q, @Nullable Integer skip, @Nullable Integer limit);

    @Override
    void aggregated(Writer w, Field.AggregatedField ag);

    @Deprecated(since = "should not impl")
    @Override
    default void assign(Writer w, Field<?> left, Object value) {
        throw new IllegalStateException("should never be called for SQL");
    }

    @Override
    void between(Writer w, Value<?> left, Object l, Object h);

    @Override
    void bitwiseAnd(Writer w, Value<?> left, Object right, int bits);

    @Override
    void bitwiseNot(Writer w, Value<?> left, int bits);

    @Override
    void bitwiseOr(Writer w, Value<?> left, Object right, int bits);

    @Override
    void bitwiseShiftLeft(Writer w, Value<?> left, Object right, int bits);

    @Override
    void bitwiseShiftRight(Writer w, Value<?> left, Object right, int bits);

    @Override
    void bitwiseXor(Writer w, Value<?> left, Object right, int bits);

    @Override
    void contains(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    @Override
    void endsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    @Override
    void equal(Writer w, Value<?> left, Object right);

    @Override
    void equalCaseInsensitive(Writer w, Value<?> left, Object right);

    @Override
    void field(Writer w, Field<?> field);

    @Override
    void greater(Writer w, Value<?> left, Object right);

    @Override
    void greaterOrEqual(Writer w, Value<?> left, Object right);

    @Override
    void in(Writer w, Value<?> left, Object[] right);

    @Override
    void innerJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    @Override
    void isFalse(Writer w, Value<?> left);

    @Override
    void isNotNull(Writer w, Value<?> left);

    @Override
    void isNull(Writer w, Value<?> left);

    @Override
    void isTrue(Writer w, Value<?> left);

    @Override
    void jsonMerge(Writer w, Field<?> left, Object[] path, Object value);

    @Override
    void jsonPath(Writer w, Value.JsonValue<?> root, List<JsonGet.Path> path);

    @Override
    void jsonRemove(Writer w, Field<?> left, Object[] path);

    @Override
    void jsonSet(Writer w, Field<?> left, Object[] path, Object value);

    @Override
    void leftOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    @Override
    void lesser(Writer w, Value<?> left, Object right);

    @Override
    void lesserOrEqual(Writer w, Value<?> left, Object right);

    @Override
    void logicalAnd(Writer w, Value<?> left, Object right);

    @Override
    void logicalNot(Writer w, Value<?> left);

    @Override
    void logicalOr(Writer w, Value<?> left, Object right);

    @Override
    void mathDivide(Writer w, Value<?> left, Object right, int bits);

    @Override
    void mathMinus(Writer w, Value<?> left, Object right, int bits);

    @Override
    void mathNegative(Writer w, Value<?> left, int bits);

    @Override
    void mathPlus(Writer w, Value<?> left, Object right, int bits);

    @Override
    void mathReminder(Writer w, Value<?> left, Object right, int bits);

    @Override
    void mathTimes(Writer w, Value<?> left, Object right, int bits);

    @Override
    void model(Writer w, Model<?> model);

    @Override
    void noneEqual(Writer w, Value<?> left, Object right);

    @Override
    void notEqualCaseInsensitive(Writer w, Value<?> left, Object right);

    @Override
    void notIn(Writer w, Value<?> left, Object[] right);

    @Override
    void order(Writer w, Field<?> field, boolean desc);

    @Deprecated(since = "should not impl")
    @Override
    default boolean registerPlaceHolder(Writer w, String name, Class<?> type) {
        return false;
    }

    @Override
    void rightOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    @Override
    default void set(Writer w, Field<?> left, Object value) {
        var ow = left._onWrite(); //! handle write interceptor
        w.render(left).w("=");
        if (ow != null && !(value instanceof Renderable)) {
            w.render(ow.apply(value));
        } else {
            w.render(value);
        }
    }

    @Override
    void startsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    @Override
    void virtualField(Writer w, Field<?> field);
}
