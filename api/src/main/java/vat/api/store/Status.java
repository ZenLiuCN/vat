package vat.api.store;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.jspecify.annotations.Nullable;
import vat.api.DomainError;
import vat.api.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-10-23


public record Status(State state) {
    static <T> Function<Throwable, Future<T>> recovery(String query, Object parameters, UnaryOperator<Throwable> except) {
        return ex -> {
            Store.log.error("execute {} : {} fail", query, parameters, ex);
            return Future.failedFuture(except.apply(ex));
        };
    }

    @SuppressWarnings("unchecked")
    static <V> @Nullable V first(List<?> v) {
        return v.isEmpty() ? null : (V) v.getFirst();
    }

    static <V> List<@Nullable V> toFirstList(SqlResult<? extends List<?>> v) {
        var out = new ArrayList<@Nullable V>();
        while (v != null) {
            var val = v.value();
            out.add(first(val));
            v = v.next();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static <V> V mustFirst(@Nullable List<?> v) {
        if (v == null || v.size() != 1) throw DomainError.System.conflict("require one value");
        return (V) v.getFirst();
    }

    @SuppressWarnings("unchecked")
    static <V> List<V> cast(List<?> v) {
        return (List<V>) v;
    }

    public Rendered render(QueryType type) {
        state.type = type;
        return state.doRender();
    }

    public void withHistory() {
        state.withHistory = true;
    }

    public <T> Stages.Store<T> store() {
        return () -> this;
    }

    private <T> Future<T> executeOne(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        var reader = rendered.reader;
        Store.log.debug("One Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forQuery(state.sql, query)
                .collecting(Collectors.mapping(reader::read, Collectors.toList()))
                .execute(rendered.parameter)
                .map(SqlResult::value)
                .map(Status::<T>mustFirst)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private <T> Future<T> executeFirst(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        var reader = rendered.reader;
        Store.log.debug("First maybe Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forQuery(state.sql, query)
                .collecting(Collectors.mapping(reader::read, Collectors.toList()))
                .execute(rendered.parameter)
                .map(SqlResult::value)
                .map(Status::<T>first)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private Future<Integer> executeCounting(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("Counting Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forQuery(state.sql, query)
                .collecting(Collectors.mapping(x -> x.getInteger(0), Collectors.toList()))
                .execute(rendered.parameter)
                .map(SqlResult::value)
                .map(List::getFirst)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private <T> Future<List<T>> executeAny(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("Any Query {} : {}", query, rendered.parameter);
        var reader = rendered.reader;
        return SqlTemplate.forQuery(state.sql, query)
                .collecting(Collectors.mapping(reader::read, Collectors.toList()))
                .execute(rendered.parameter)
                .map(SqlResult::value)
                .map(Status::<T>cast)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private Future<Integer> executeRemove(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("Remove Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forUpdate(state.sql, query)
                .execute(rendered.parameter)
                .map(SqlResult::rowCount)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private Future<Boolean> executeInsertOne(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("InsertOne Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forUpdate(state.sql, query)
                .execute(rendered.parameter)
                .map(x -> x.rowCount() == 1)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }

    private Future<Integer> executeInsertMany(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("InsertMany Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forUpdate(state.sql, query)
                .executeBatch(rendered.parameters)
                .map(SqlResult::rowCount)
                .recover(recovery(query, rendered.parameters, state.dialect::exceptionHandler));
    }

    private <ID> Future<ID> executeInsertOneReturningID(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        var returns = state.dialect.returning(state);
        Store.log.debug("InsertOneReturningID Query {} : {}", query, rendered.parameter);
        if (returns == null) {
            var reader = state.primary._identity()._reader().toReader(0);
            return SqlTemplate.forQuery(state.sql, query)
                    .collecting(Collectors.mapping(reader::read, Collectors.toList()))
                    .execute(rendered.parameter)
                    .map(SqlResult::value)
                    .map(Status::<ID>first)
                    .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
        }
        return SqlTemplate.forUpdate(state.sql, query)
                .execute(rendered.parameter)
                .flatMap(x -> returns.process(state.sql, x, rendered.reader))
                .map(Status::<ID>first)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));

    }

    private <T> Future<List<T>> executeInsertManyReturningID(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        var returns = state.dialect.returning(state);
        Store.log.debug("InsertManyReturningID Query {} : {}", query, rendered.parameters);
        if (returns == null) {
            var reader = state.primary._identity()._reader().toReader(0);
            return SqlTemplate.forQuery(state.sql, query)
                    .collecting(Collectors.mapping(reader::read, Collectors.toList()))
                    .executeBatch(rendered.parameters)
                    .map(Status::<T>toFirstList)
                    .recover(recovery(query, rendered.parameters, state.dialect::exceptionHandler));
        }
        return SqlTemplate.forUpdate(state.sql, query)
                .executeBatch(rendered.parameters)
                .flatMap(x -> returns.process(state.sql, x, rendered.reader))
                .map(Status::<T>cast)
                .recover(recovery(query, rendered.parameters, state.dialect::exceptionHandler));
    }

    private Future<Integer> executeUpdate(Rendered rendered) {
        var query = rendered.query.toString();
        if (!rendered.placeHolder.isEmpty())
            return Future.failedFuture(new IllegalStateException("query contains placeholder"));
        Store.log.debug("Update Query {} : {}", query, rendered.parameter);
        return SqlTemplate.forUpdate(state.sql, query)
                .execute(rendered.parameter)
                .map(SqlResult::rowCount)
                .recover(recovery(query, rendered.parameter, state.dialect::exceptionHandler));
    }


    public <T> Future<T> one() {
        state.type = QueryType.SELECT_ONE;
        var rendered = state.doRender();
        return executeOne(rendered);
    }

    public <T> Future<T> first() {
        state.type = QueryType.SELECT_ONE;
        var rendered = state.doRender();
        return executeFirst(rendered);
    }

    public Future<Integer> count() {
        state.type = QueryType.COUNTING;
        var rendered = state.dialect.render(state, new Rendered(state.dialect));
        return executeCounting(rendered);
    }


    public <T> Future<List<T>> any() {
        state.type = QueryType.SELECT_ANY;
        var rendered = state.doRender();
        return executeAny(rendered);
    }


    public <T> Future<List<T>> slice(int skip, int maximum) {
        state.type = QueryType.SELECT_ANY;
        state.skip = skip;
        state.limit = maximum;
        var rendered = state.doRender();
        return executeAny(rendered);
    }


    public Future<Integer> remove(boolean permanent) {
        state.type = QueryType.DELETE;
        state.permanent = permanent;
        var rendered = state.doRender();
        return executeRemove(rendered);
    }

    public Future<Boolean> justPut(StmtAssign[] assigns) {
        state.type = QueryType.INSERT;
        state.assigns = assigns;
        var rendered = state.doRender();
        return executeInsertOne(rendered);
    }

    public <ID> Future<ID> put(StmtAssign[] assigns) {
        state.type = QueryType.INSERT_RETURNS;
        state.assigns = assigns;
        var rendered = state.doRender();
        return executeInsertOneReturningID(rendered);
    }

    public Future<Integer> justPutMany(StmtAssign[][] assigns) {
        state.type = QueryType.INSERT_MULTI;
        state.assignsMany = assigns;
        var rendered = state.doRender();
        return executeInsertMany(rendered);
    }

    public <ID> Future<List<ID>> putMany(StmtAssign[][] assigns) {
        state.type = QueryType.INSERT_MULTI_RETURNS;
        state.assignsMany = assigns;
        var rendered = state.doRender();
        return executeInsertManyReturningID(rendered);
    }

    public Future<Integer> justSet(Statement.SetStmt[] sets) {
        state.type = QueryType.UPDATE;
        state.sets = sets;
        var rendered = state.doRender();
        return executeUpdate(rendered);
    }


    static final Reader<JsonObject> JsonObjectReader = r -> {
        var j = new JsonObject();
        for (int i = 0; i < r.size(); i++) {
            j.put(r.getColumnName(i), r.getValue(i));
        }
        return j;
    };

    public <R> Future<R> one(Function<JsonObject, R> mapper) {
        state.type = QueryType.SELECT_ONE;
        var rendered = state.dialect.render(state, new Rendered(state.dialect));
        rendered.reader = JsonObjectReader.map(mapper);
        return executeOne(rendered);
    }

    public <R> Future<R> first(Function<JsonObject, R> mapper) {
        state.type = QueryType.SELECT_ONE;
        var rendered = state.dialect.render(state, new Rendered(state.dialect));
        rendered.reader = JsonObjectReader.map(mapper);
        return executeFirst(rendered);
    }

    public <R> Future<List<R>> any(Function<JsonObject, R> mapper) {
        state.type = QueryType.SELECT_ANY;
        var rendered = state.dialect.render(state, new Rendered(state.dialect));
        rendered.reader = JsonObjectReader.map(mapper);
        return executeAny(rendered);
    }

    public <T> Future<List<T>> top(int limit) {
        state.type = QueryType.SELECT_ANY;
        state.limit = limit;
        var rendered = state.dialect.render(state, new Rendered(state.dialect));
        return executeAny(rendered);
    }

    public Stages.Picked<?> pick(Value<?>... field) {
        for (var value : field) {
            if (value instanceof Field<?>) continue;
            if (value instanceof JsonGet<?, ?>) continue;
            throw new IllegalStateException("only JsonGet or Field can picked:" + value.getClass());
        }
        state.pick(field);
        return () -> this;
    }

    public Stages.Joined.Joined<?> join(Model<?> model, Value.BooleanValue condition) {
        state.join(new StmtJoin(StmtJoin.Mode.INNER, model, condition));
        return castJoin();
    }

    @SuppressWarnings("RedundantCast")
    private Stages.Joined<?> castJoin() {
        assert state.joined != null;
        return switch (state.joined.size()) {
            case 1 -> ((Stages.Joined.Joined2<?, ?>) () -> this);
            case 2 -> ((Stages.Joined.Joined3<?, ?, ?>) () -> this);
            case 3 -> ((Stages.Joined.Joined4<?, ?, ?, ?>) () -> this);
            case 4 -> ((Stages.Joined.Joined5<?, ?, ?, ?, ?>) () -> this);
            case 5 -> ((Stages.Joined.Joined6<?, ?, ?, ?, ?, ?>) () -> this);
            case 6 -> ((Stages.Joined.Joined7<?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 7 -> ((Stages.Joined.Joined8<?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 8 -> ((Stages.Joined.Joined9<?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 9 -> ((Stages.Joined.Joined10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 10 -> ((Stages.Joined.Joined11<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 11 -> ((Stages.Joined.Joined12<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 12 -> ((Stages.Joined.Joined13<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 13 -> ((Stages.Joined.Joined14<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 14 -> ((Stages.Joined.Joined15<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            case 15 -> ((Stages.Joined.Joined16<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) () -> this);
            default -> throw new IllegalStateException("unsupported join size" + state.joined.size());
        };
    }

    public Stages.Joined<?> joinWith(Model<?> model, Value.BooleanValue condition) {
        state.join(new StmtJoin(StmtJoin.Mode.LEFT_OUTER, model, condition));
        return castJoin();
    }

    public Stages.Joined<?> joinTo(Model<?> model, Value.BooleanValue condition) {
        state.join(new StmtJoin(StmtJoin.Mode.RIGHT_OUTER, model, condition));
        return castJoin();
    }


    public void withActor(@Nullable Object actor) {
        state.actor = actor;
    }

    public Stages.Filtered<?> filter(Value.BooleanValue condition) {
        state.filter(condition);
        return () -> this;
    }

    public Stages.Grouped<?> grouped(Field<?> field, Field<?>[] extra) {
        if (extra.length == 0)
            state.grouped = new Field[]{field};
        else {
            var fx = new Field[extra.length + 1];
            fx[0] = field;
            System.arraycopy(extra, 0, fx, 1, extra.length);
            state.grouped = fx;
        }
        return () -> this;
    }

    public Stages.FilterGroup<?> having(Value.BooleanValue condition) {
        if (state.having == null)
            state.having = condition;
        else
            state.having = state.having.and(condition);
        return () -> this;
    }

    public Stages.Sorted<?> sorted(StmtOrder order, StmtOrder[] extra) {
        if (extra.length == 0)
            state.ordered = new StmtOrder[]{order};
        else {
            var fx = new StmtOrder[extra.length + 1];
            fx[0] = order;
            System.arraycopy(extra, 0, fx, 1, extra.length);
            state.ordered = fx;
        }
        return () -> this;
    }
}
