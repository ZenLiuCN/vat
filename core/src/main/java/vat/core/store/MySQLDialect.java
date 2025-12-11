package vat.core.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.*;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import vat.api.store.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static vat.api.store.StoreError.ErrorCode.*;

///
/// @author Zen.Liu
/// @since 2025-10-24

@Slf4j
public class MySQLDialect implements Dialect, PoolMaker {

    public  void config(){
        log.debug("initialize MySQL specific settings");
        Field.INSTANT_OFFSET_MODE.set(false);
        Field.SUPPORT_OFFSET_TIME.set(false);
        Field.OFFSET_TIMEZONE.set(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        Field.OFFSET_INSTANT.set(ZoneOffset.UTC);
        log.trace("initialized MySQL {}:{}, {}:{}"
                ,Field.INSTANT_OFFSET_MODE
                ,Field.OFFSET_INSTANT
                ,Field.SUPPORT_OFFSET_TIME
                ,Field.OFFSET_TIMEZONE
        );
    }
    @Override
    public String name() {
        return "MySQL";
    }

    @Override
    public List<Statement.SetStmt> updateAuditInjects(State state) {
        var extra = new ArrayList<Statement.SetStmt>();
        var fields = new HashSet<Field<?>>();
        if (state.primary._historic()) {
            if (state.sets != null)
                for (var set : state.sets) {
                    if (set instanceof StmtSet<?> s) fields.add(s.left());
                    else if (set instanceof StmtJsonSet s) fields.add(s.left());
                }

            historicInject(state, fields, extra);
        }
        if (state.primary._auditable()) {
            var tab = state.primary;
            var actor = state.actor;
            if (fields.isEmpty() && state.sets != null)
                for (var set : state.sets) {
                    if (set instanceof StmtSet<?> s) fields.add(s.left());
                    else if (set instanceof StmtJsonSet s) fields.add(s.left());
                }
            var modifier = TraitField.MODIFIER.of(tab);
            if (modifier instanceof Value.NumberValue<?> && actor == null) actor = -1;
            if (!fields.contains(modifier))
                extra.add(new StmtSet<>(modifier, actor));
        }
        if (state.primary._optLock()) {
            if (fields.isEmpty() && state.sets != null)
                for (var set : state.sets) {
                    if (set instanceof StmtSet<?> s) fields.add(s.left());
                    else if (set instanceof StmtJsonSet s) fields.add(s.left());
                }

            Field.IntegerField version = TraitField.VERSION.of(state.primary);
            if (version != null && !fields.contains(version)) {
                extra.add(new StmtSet<>(version, version.plus(1)));
            }
        }

        return extra;
    }

    @Override
    public void historicInject(State state, Set<Field<?>> fieldSet, List<Statement.SetStmt> extra) {
        var h = TraitField.HISTORY.of(state.primary);
        if (fieldSet.contains(h)) {
            return;
        }
        var id = state.primary._identity();
        extra.add(new StmtJsonSet(StmtJsonSet.Operator.SET,
                h,
                new Object[]{Instant.now().toString(),},
                Renderable.render((r, w) -> w.w("JSON_OBJECT(")
                        .each(Arrays.stream(state.primary._fields())
                                        .filter(v -> !v.equals(h) && !v.equals(id)),
                                ',', (x, v) -> x.quote(v._name()).w(',').backQuote(v._name()))
                        .w(")")
                )));

    }

    @Override
    public List<StmtAssign> insertAuditInjects(State state) {
        var extra = new ArrayList<StmtAssign>();
        if (state.primary._auditable()) {
            var fields = new HashSet<Field<?>>();
            var tab = state.primary;
            var actor = state.actor;
            for (var assign : state.assignsMany[0]) {
                fields.add(assign.left());
            }
            var creator = TraitField.CREATOR.of(tab);
            if (creator instanceof Value.NumberValue<?> && actor == null) {
                actor = -1;
            }
            if (!fields.contains(creator)) extra.add(new StmtAssign(creator, actor));
            var modifier = TraitField.MODIFIER.of(tab);
            if (!fields.contains(modifier))
                extra.add(new StmtAssign(modifier, actor));
            if (state.primary._historic()) {
                var his = TraitField.HISTORY.of(state.primary);
                if (his != null && !fields.contains(his))
                    extra.add(new StmtAssign(his, JsonObject.of()));
            }
        } else if (state.primary._historic()) {
            var his = TraitField.HISTORY.of(state.primary);
            if (his != null) {
                var fields = new HashSet<Field<?>>();
                for (var assign : state.assignsMany[0]) {
                    fields.add(assign.left());
                }
                if (!fields.contains(his)) {
                    extra.add(new StmtAssign(his, JsonObject.of()));
                }
            }
        }
        return extra;
    }

    @Override
    public void insertAuditInject(State state) {
        if (state.primary._auditable()) {
            var tab = state.primary;
            var actor = state.actor;
            var fields = Arrays.stream(state.assigns).map(StmtAssign::left).collect(Collectors.toSet());
            var newAssign = new ArrayList<>(Arrays.asList(state.assigns));
            var creator = TraitField.CREATOR.of(tab);
            if (creator instanceof Value.NumberValue<?> && actor == null) {
                actor = -1;
            }
            if (!fields.contains(creator) && actor != null)
                newAssign.add(new StmtAssign(creator, actor));
            var modifier = TraitField.MODIFIER.of(tab);
            if (!fields.contains(modifier) && actor != null)
                newAssign.add(new StmtAssign(modifier, actor));
            if (tab._historic()) {
                var his = TraitField.HISTORY.of(tab);
                if (his != null && !fields.contains(his))
                    newAssign.add(new StmtAssign(his, JsonObject.of()));
            }
            state.assigns = newAssign.toArray(new StmtAssign[0]);
        } else if (state.primary._historic()) {
            var tab = state.primary;
            var fields = Arrays.stream(state.assigns).map(StmtAssign::left).collect(Collectors.toSet());
            var his = TraitField.HISTORY.of(tab);
            if (his != null && !fields.contains(his)) {
                var newAssign = new ArrayList<>(Arrays.asList(state.assigns));
                newAssign.add(new StmtAssign(his, JsonObject.of()));
                state.assigns = newAssign.toArray(new StmtAssign[0]);
            }

        }
    }

    @Override
    public void insert(Writer q, State state, Rendered rendered, boolean returning) {
        insertAuditInject(state);
        rendered.stage = Stage.INSERT;
        q.w("INSERT INTO").sp().use(w -> state.primary._render(rendered, w));
        rendered.stage = Stage.COLUMNS;
        q.sp().w('(').each(state.assigns, StmtAssign::left, ',', rendered::render).w(')');
        rendered.stage = Stage.VALUES;
        q.wp("VALUES").w('(').each(state.assigns, ',', writerConsumeValue(rendered::render)).w(')');
    }

    @Override
    public void insertMulti(Writer q, State state, Rendered rendered, boolean returning) {
        var pm = new ArrayList<Map<String, Object>>();
        var extra = insertAuditInjects(state);
        var n = 0;
        for (var stmtAssigns : state.assignsMany) {
            if (n == 0) n = stmtAssigns.length;
            else if (n != stmtAssigns.length)
                throw new IllegalStateException(
                        "parameter dimensions not match:" + Arrays.deepToString(state.assignsMany));
            var m = new HashMap<String, Object>(stmtAssigns.length);
            var i = 0;
            for (var stmtAssign : stmtAssigns) {
                m.put("V" + i, parameter(q, writerApply(stmtAssign)));
                i++;
            }
            for (var ex : extra) {
                m.put("V" + i, parameter(q, writerApply(ex)));
                i++;
            }
            pm.add(m);
        }
        rendered.parameters = pm;
        rendered.stage = Stage.INSERT;
        q.w("INSERT INTO").sp().use(w -> state.primary._render(rendered, w));
        rendered.stage = Stage.COLUMNS;
        q.sp().w('(').each(state.assignsMany[0], StmtAssign::left, ',', rendered::render).w(')');
        rendered.stage = Stage.VALUES;
        q.wp("VALUES").w('(').range(n, ',', (w, v) -> w.p("V" + v)).w(')');
    }

    @Override
    public void update(Writer q, State state, Rendered rendered) {
        rendered.stage = Stage.UPDATE;
        var an = new HashMap<Model<?>, String>();
        //* pre compute alias
        if (state.joined != null && !state.joined.isEmpty()) {
            if (state.primary._alias() == null)
                state.primary._alias(an.computeIfAbsent(state.primary, m -> "T" + 0));
            if (state.joined != null && !state.joined.isEmpty())
                state.joined = state.joined.stream().map(v -> {
                    if (v.model()._alias() == null) {
                        return v.withAlias(an.computeIfAbsent(v.model(), m -> "T" + an.size()));
                    } else {
                        an.put(v.model(), v.model()._alias());
                        return v;
                    }
                }).toList();
        }
        q.w("UPDATE").sp().use(w -> state.primary._render(rendered, w)).sp();
        q.w("SET").sp();
        rendered.stage = Stage.SET;
        var extra = updateAuditInjects(state);
        if (!extra.isEmpty()) {
            q.each(extra, ',', writerJsonConsume('=', rendered::render)).w(',');
        }
        q.each(state.sets, ',', writerJsonConsume('=', rendered::render));
        if (state.joined != null && !state.joined.isEmpty()) {
            rendered.stage = Stage.JOIN;
            //! postgresql mode
            q.wp("FROM").each(state.joined, ' ', rendered::render);
        }
        whereBuilder(q, state, rendered, false);
    }

    @Override
    public void delete(Writer q, State state, Rendered rendered) {
        rendered.stage = Stage.DELETE;
        q.w("DELETE").wp("FROM").use(w -> state.primary._render(rendered, w));
        whereBuilder(q, state, rendered, true);
    }

    @Override
    public void softDelete(Writer q, State state, Rendered rendered) {
        rendered.stage = Stage.UPDATE;
        var an = new HashMap<Model<?>, String>();
        //* pre compute alias
        if (state.joined != null && !state.joined.isEmpty()) {
            if (state.primary._alias() == null)
                state.primary._alias(an.computeIfAbsent(state.primary, m -> "T" + 0));
            if (state.joined != null && !state.joined.isEmpty())
                state.joined = state.joined.stream().map(v -> {
                    if (v.model()._alias() == null) {
                        return v.withAlias(an.computeIfAbsent(v.model(), m -> "T" + an.size()));
                    } else {
                        an.put(v.model(), v.model()._alias());
                        return v;
                    }
                }).toList();
        }
        q.w("UPDATE").sp().use(w -> state.primary._render(rendered, w));
        q.wp("SET");
        rendered.stage = Stage.SET;
        var extra = updateAuditInjects(state);
        if (!extra.isEmpty()) {
            q.each(extra, ',', writerJsonConsume('=', rendered::render)).w(',');
        }
        rendered.render(q, new StmtSet<>(TraitField.REMOVED.of(state.primary), true));
        whereBuilder(q, state, rendered, false);
    }

    @Override
    public @Nullable ReturningProcessor returning(State state) {
        return switch (state.type) {
            case INSERT_RETURNS -> (s, r, re) ->
                    Future.succeededFuture(List.of(r.property(MySQLClient.LAST_INSERTED_ID)));
            case INSERT_MULTI_RETURNS -> (s, r, re) -> {
                var lst = new ArrayList<>();
                while (r != null) {
                    lst.add(r.property(MySQLClient.LAST_INSERTED_ID));
                    r = r.next();
                }
                return Future.succeededFuture(lst);
            };
            default -> throw new IllegalStateException("unsupported returning");
        };
    }


    @Override
    public Object parameter(Writer w, Object value) {
        if (w.expr() instanceof StmtJsonSet) {
            if (value instanceof Boolean b) {
                return b ? "true" : "false";
            }
            return value instanceof String s
                    ? Json.encode(s)
                    : value;
        }
        return Dialect.super.parameter(w, value);
    }


    @Override
    public Throwable exceptionHandler(Throwable e) {

        if (e instanceof MySQLException pe) {
            return switch (pe.getErrorCode()) {
                // Constraint violations
                case 1062 -> // ER_DUP_ENTRY - Duplicate entry
                        new StoreError(DATA_VIOLATION_DUPLICATED, "Duplicate entry found", pe);
                case 1452 -> // ER_NO_REFERENCED_ROW - Foreign key violation
                        new StoreError(DATA_VIOLATION_MISSING_FK, "Foreign key constraint violation", pe);
                case 1048 -> // ER_BAD_NULL_ERROR - Not null violation
                        new StoreError(DATA_VIOLATION_NULL_VALUE, "Not null constraint violation", pe);
                case 3819 -> // ER_CHECK_CONSTRAINT_VIOLATED - Check constraint violation
                        new StoreError(DATA_VIOLATION_CONSTRAINT, "Check constraint violation", pe);

                // Syntax and invalid operations
                case 1064 -> // ER_PARSE_ERROR - SQL syntax error
                        new StoreError(GRAMMAR_SYNTAX, "SQL syntax error", pe);
                case 1305 -> // ER_SP_DOES_NOT_EXIST - Undefined function/procedure
                        new StoreError(GRAMMAR_FUNCTION, "Undefined function or procedure", pe);
                case 1146 -> // ER_NO_SUCH_TABLE - Undefined table
                        new StoreError(GRAMMAR_TABLE, "Undefined table", pe);
                case 1054 -> // ER_BAD_FIELD_ERROR - Undefined column
                        new StoreError(GRAMMAR_COLUMN, "Undefined column", pe);

                // Data exceptions
                case 1406 -> // ER_DATA_TOO_LONG - Data too long
                        new StoreError(DATA_ACCESS_OVERFLOW, "Data too long for column", pe);
                case 1264 -> // ER_WARN_DATA_OUT_OF_RANGE - Numeric value out of range
                        new StoreError(DATA_ACCESS_RANG, "Numeric value out of range", pe);
                case 1292 -> // ER_TRUNCATED_WRONG_VALUE - Invalid value format
                        new StoreError(DATA_ACCESS_FORMAT, "Invalid value format", pe);

                // Connection and transaction issues
                // ER_CON_COUNT_ERROR - Too many connections
                // CR_CONN_HOST_ERROR - Cannot connect to server
                case 1040, 2002, 2003 -> // CR_CONNECTION_ERROR - Connection error
                        new StoreError(CONNECTION_ERROR, "Connection issue: " + pe.getMessage(), pe);

                // Deadlock and lock issues
                case 1213 -> // ER_LOCK_DEADLOCK - Deadlock detected
                        new StoreError(LOCK_DEAD_LOCK, "Deadlock detected", pe);
                case 1205 -> // ER_LOCK_WAIT_TIMEOUT - Lock wait timeout
                        new StoreError(LOCK_WAIT_TIMEOUT, "Lock wait timeout", pe);

                // Permission and security issues
                // ER_DBACCESS_DENIED_ERROR - Access denied
                case 1044, 1045 -> // ER_ACCESS_DENIED_ERROR - Access denied
                        new StoreError(PERMISSION_ERROR, "Access denied", pe);

                // Resource and system limits
                // Out of resources
                // Out of resources
                case 23, 24, 145 ->  // ER_TABLE_NEEDS_UPGRADE - Table needs upgrade
                        new StoreError(RESOURCE_LIMIT, "Resource limit exceeded: " + pe.getMessage(), pe);
                default -> switch (pe.getSqlState()) {
                    // Standard SQL states (MySQL sometimes uses these)
                    case "23000" -> // Integrity constraint violation
                            new StoreError(DATA_VIOLATION_CONSTRAINT, "Integrity constraint violation", pe);
                    case "42000" -> // Syntax error or access violation
                            new StoreError(GRAMMAR_ERROR, "Syntax error or access violation", pe);
                    case "HY000" -> // General error
                            new StoreError(OTHER_ERROR, "General MySQL error", pe);
                    case "08S01" -> // Communication link failure
                            new StoreError(CONNECTION_ERROR, "Communication link failure", pe);
                    case "40001" -> // Serialization failure
                            new StoreError(CONCURRENCY_ERROR, "Serialization failure", pe);
                    default -> new StoreError(OTHER_ERROR,
                            "Uncategorized MySQL error " + pe.getSqlState(), pe
                    );
                };
            };
        } else if (e instanceof MySQLBatchException pe) {
            return new StoreError(MULTI_ERROR, "batch error", pe);
        }
        return e;

    }


    @Override
    public void limits(Writer q, @Nullable Integer skip, @Nullable Integer limit) {
        q
                .when(limit, (w, v) -> w.wp("LIMIT %d".formatted(v)))
                .when(skip, (w, v) -> w.wp("OFFSET %d".formatted(v)))
        ;
    }

    @Override
    public void aggregated(Writer w, Field.AggregatedField ag) {
        throw new IllegalStateException("not implemented");
    }


    @Override
    public void rawValue(Writer w, RawValue<?> tRawValue) {
        w.w(tRawValue.code());
    }

    @Override
    public void virtualHistory(Writer w, Field<?> field) {
        w.w("'{}' ");
        if (field._owner() != null) {
            model(w, field._owner());
            w.dot();
        }
        if (field._alias() == null) w.backQuote(field._name());
        else w.backQuote(field._alias());
    }

    @Override
    public void between(Writer w, Value<?> left, Object l, Object h) {
        w.render(left).wp("BETWEEN").render(l).wp("AND").render(h);
    }

    @Override
    public void bitwiseAnd(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")&(").render(right).w("))");
    }

    @Override
    public void bitwiseNot(Writer w, Value<?> left, int bits) {
        w.w('(').w('~').w('(').render(left).w(')').w(')');
    }

    @Override
    public void bitwiseOr(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")|(").render(right).w("))");
    }

    @Override
    public void bitwiseShiftLeft(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")<<(").render(right).w("))");
    }

    @Override
    public void bitwiseShiftRight(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")>>(").render(right).w("))");
    }

    @Override
    public void bitwiseXor(Writer w, Value<?> left, Object right, int bits) {
        w.w('(').w('#').w('(').render(left).w(')').w(')');
    }

    @Override
    public void logicalAnd(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")AND(").render(right).w("))");
    }

    @Override
    public void logicalNot(Writer w, Value<?> left) {
        w.w("(NOT(").render(left).w("))");
    }

    @Override
    public void logicalOr(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")OR(").render(right).w("))");
    }


    @Override
    public void greater(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")>(").render(right).w("))");
    }

    @Override
    public void greaterOrEqual(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")>=(").render(right).w("))");
    }

    @Override
    public void lesser(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")<(").render(right).w("))");
    }

    @Override
    public void lesserOrEqual(Writer w, Value<?> left, Object right) {
        w.w("((").render(left).w(")<=(").render(right).w("))");
    }


    @Override
    public void mathDivide(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")/(").render(right).w("))");
    }

    @Override
    public void mathMinus(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")-(").render(right).w("))");
    }

    @Override
    public void mathNegative(Writer w, Value<?> left, int bits) {
        w.w("(-(").render(left).w("))");
    }

    @Override
    public void mathPlus(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")+(").render(right).w("))");
    }

    @Override
    public void mathReminder(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")%(").render(right).w("))");
    }

    @Override
    public void mathTimes(Writer w, Value<?> left, Object right, int bits) {
        w.w("((").render(left).w(")*(").render(right).w("))");
    }

    @Override
    public void in(Writer w, Value<?> left, Object[] right) {
        w.w("((").render(left).w(")IN(").each(right, ',', Writer::render).w("))");
    }

    @Override
    public void notIn(Writer w, Value<?> left, Object[] right) {
        w.w("((").render(left).w(")NOT IN(").each(right, ',', Writer::render).w("))");
    }


    @Override
    public void isFalse(Writer w, Value<?> left) {
        w.render(left).w(" = false");
    }

    @Override
    public void isNotNull(Writer w, Value<?> left) {
        w.render(left).w(" is not null");
    }

    @Override
    public void isNull(Writer w, Value<?> left) {
        w.render(left).w(" is null");
    }

    @Override
    public void isTrue(Writer w, Value<?> left) {
        w.render(left).w(" = true");
    }


    @Override
    public void jsonPath(Writer w, Value.JsonValue<?> root, List<JsonGet.Path> path) {
        assert root instanceof Field<?> : "not a field";
        w.render(root).w("->'").apply(x -> jsonPath(x, path.stream().map(JsonGet.Path::path).toArray())).w('\'');
    }

    @Override
    public void jsonRemove(Writer w, Field<?> left, Object[] path) {
        w
                .backQuote(left._name())
                .w("= JSON_REMOVE(")
                .backQuote(left._name())
                .w(",'")
                .apply(x -> jsonPath(x, path))
                .w("')");
    }

    @Override
    public void jsonMerge(Writer w, Field<?> left, Object[] path, Object value) {
        var wx = new Writer(null);
        jsonPath(wx, path);
        var p = wx.builder().toString();
        w
                .backQuote(left._name())
                .w("= JSON_SET(")
                .backQuote(left._name())
                .w(",'")
                .w(p)
                .w("',")
                .w("JSON_MERGE_PRESERVE(JSON_EXTRACT(") //! for same product as Postgres
                .backQuote(left._name())
                .w(",'")
                .w(p)
                .w("'),CAST(")
                .render(value)
                .w(" AS JSON)")
                .w("))");
    }

    @Override
    public void jsonSet(Writer w, Field<?> left, Object[] path, Object value) {
        w
                .backQuote(left._name())
                .w("= JSON_SET(")
                .backQuote(left._name())
                .w(",'")
                .apply(x -> jsonPath(x, path))
                .w("',CAST(")
                .render(value)
                .w(" AS JSON))");
    }

    static void jsonPath(Writer w, Object[] path) {
        w.w("$.").each(path, '.', (x, p) -> {
            if (p instanceof String k) {
                x.dblQuote(k);
            } else if (p instanceof Number l) {
                x.w(l.intValue());
            } else throw new IllegalStateException("json path can only integer or string:" + p);
        });
    }


    @Override
    public void innerJoin(Writer w, Model<?> model, Value.BooleanValue cond) {
        w.w("INNER JOIN  ").render(model)
                .when(cond, x -> x.w(" ON "), Writer::render, null)
                .w("");
    }

    @Override
    public void leftOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond) {
        w.w("LEFT OUTER JOIN   ").render(model)
                .when(cond, x -> x.w(" ON "), Writer::render, null)
                .w("");
    }

    @Override
    public void rightOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond) {
        w.w("RIGHT OUTER JOIN   ").render(model)
                .when(cond, x -> x.w(" ON "), Writer::render, null)
                .w("");
    }

    @Override
    public void model(Writer w, Model<?> model) {
        switch (w.rendered().stage) {
            case SELECT,
                 COUNTING,
                 WHERE,
                 ON,
                 GROUP_BY,
                 HAVING,
                 ORDER_BY,
                 LIMITS,
                 COLUMNS,
                 VALUES,
                 SET,
                 RETURNING -> {
                if (model._alias() != null) {
                    w.backQuote(model._alias());
                } else {
                    w.backQuote(model._name());
                }
            }
            case INSERT, FROM, UPDATE, DELETE, JOIN -> w
                    .when(model._schema(), Writer::backQuote, Writer::dot)
                    .backQuote(model._name())
                    .when(model._alias(), Writer::sp, Writer::backQuote);
            default -> throw new IllegalStateException("unhandled model stage: " + w.rendered().stage);
        }
    }

    @Override
    public void field(Writer w, Field<?> field) {
        switch (w.rendered().stage()) {
            case SELECT, SET -> {
                if (field._owner() != null) {
                    model(w, field._owner());
                    w.dot();
                }
                if (field._alias() == null) w.backQuote(field._name());
                else w.backQuote(field._name()).wp("AS").backQuote(field._alias());
            }
            case LIMITS,
                 INSERT,
                 COLUMNS,
                 VALUES,
                 UPDATE,
                 DELETE,
                 RETURNING,
                 COUNTING,
                 FROM,
                 WHERE,
                 JOIN,
                 ON,
                 GROUP_BY, ORDER_BY, HAVING -> {
                if (field._owner() != null) {
                    model(w, field._owner());
                    w.dot();
                }
                if (field._alias() == null) {
                    w.backQuote(field._name());
                } else
                    w.backQuote(field._alias());
            }

        }
    }

    @Override
    public void virtualField(Writer w, Field<?> field) {
        w.render(field);
    }


    @Override
    public void order(Writer w, Field<?> field, boolean desc) {
        w.render(field).sp().when(desc, "DESC", "ASC").sp();
    }


    @Override
    public void contains(Writer w, Value<?> left, Object right, boolean caseInsensitive) {
        w
                .render(left)
                .when(caseInsensitive, "ILIKE(", "LIKE(")
                .when(right instanceof Renderable,
                        a -> a.w("CONCAT('%',").render(right).w("'%')"),
                        a -> a.quote(a.escape("%" + right.toString() + "%", '\'')))
                .w("))");
    }

    @Override
    public void endsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive) {
        w
                .render(left)
                .when(caseInsensitive, "ILIKE(", "LIKE(")
                .when(right instanceof Renderable,
                        a -> a.w("CONCAT('%',").render(right).w(")"),
                        a -> a.quote(a.escape("%" + right.toString(), '\'')))
                .w("))");
    }

    @Override
    public void noneEqual(Writer w, Value<?> left, Object right) {
        w.w("(").render(left).w("!=").render(right).w(")");
    }

    @Override
    public void notEqualCaseInsensitive(Writer w, Value<?> left, Object right) {
        w.w("(LOW(").render(left).w(")!=LOW(").render(right).w("))");
    }

    @Override
    public void equal(Writer w, Value<?> left, Object right) {
        w.w("(").render(left).w("=").render(right).w(")");
    }

    @Override
    public void equalCaseInsensitive(Writer w, Value<?> left, Object right) {
        w.w("(LOW(").render(left).w(")=LOW(").render(right).w("))");
    }

    @Override
    public void startsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive) {
        w
                .render(left)
                .when(caseInsensitive, "ILIKE(", "LIKE(")
                .when(right instanceof Renderable,
                        a -> a.w("CONCAT(").render(right).w(",'%')"),
                        a -> a.quote(a.escape(right.toString() + "%", '\'')))
                .w("))");
    }

    @Override
    public Pool make(Vertx vertx, String connection, PoolOptions options) {
        return MySQLBuilder.pool().using(vertx).connectingTo(connection).with(options).build();
    }

    @Override
    public Pool make(Vertx vertx, JsonObject connection, PoolOptions options) {
        return MySQLBuilder.pool().using(vertx).connectingTo(new MySQLConnectOptions(connection)).with(options).build();
    }
}
