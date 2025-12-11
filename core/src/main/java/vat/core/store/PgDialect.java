package vat.core.store;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
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
public class PgDialect implements Dialect, PoolMaker {
    public void config(){
        log.debug("initialize Postgres specific settings");
        Field.INSTANT_OFFSET_MODE.set(true);
        Field.SUPPORT_OFFSET_TIME.set(true);
        Field.OFFSET_TIMEZONE.set(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        Field.OFFSET_INSTANT.set(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        log.trace("initialized Postgres {}:{}, {}:{}"
                ,Field.INSTANT_OFFSET_MODE
                ,Field.OFFSET_INSTANT
                ,Field.SUPPORT_OFFSET_TIME
                ,Field.OFFSET_TIMEZONE
        );
    }
    @Override
    public String name() {
        return "Postgres";
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
            var modifiedAt = TraitField.MODIFIED_AT.of(tab);
            if (!fields.contains(modifiedAt))
                extra.add(new StmtSet<>(modifiedAt, RawValue.NOW));
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
        extra.add(new StmtJsonSet(StmtJsonSet.Operator.SET,
                h,
                new Object[]{Instant.now()},
                Renderable.render((r, w) -> w.w("(SELECT row_to_json(").dblQuote(state.primary._name()).w(") FROM ")
                        .dblQuote(state.primary._name())
                        .wp("WHERE")
                        .render(state.cond).w(")"))));
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
            var createdAt = TraitField.CREATE_AT.of(tab);
            if (!fields.contains(createdAt))
                extra.add(new StmtAssign(createdAt, RawValue.NOW));
            var modifier = TraitField.MODIFIER.of(tab);
            if (!fields.contains(modifier))
                extra.add(new StmtAssign(modifier, actor));
            var modifiedAt = TraitField.MODIFIED_AT.of(tab);
            if (!fields.contains(modifiedAt))
                extra.add(new StmtAssign(modifiedAt, RawValue.NOW));
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
            if (!fields.contains(creator))
                newAssign.add(new StmtAssign(creator, actor));
            var createdAt = TraitField.CREATE_AT.of(tab);
            if (!fields.contains(createdAt))
                newAssign.add(new StmtAssign(createdAt, RawValue.NOW));
            var modifier = TraitField.MODIFIER.of(tab);
            if (!fields.contains(modifier))
                newAssign.add(new StmtAssign(modifier, actor));
            var modifiedAt = TraitField.MODIFIED_AT.of(tab);
            if (!fields.contains(modifiedAt))
                newAssign.add(new StmtAssign(modifiedAt, RawValue.NOW));
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
        if (returning) {
            rendered.stage = Stage.RETURNING;
            rendered.reader = state.primary._identity()._reader().toReader(0);
            q.wp("RETURNING").use(state.primary._identity(), rendered::render);
        }
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
        if (returning) {
            rendered.stage = Stage.RETURNING;
            rendered.reader = Reader.combine(state.primary._create(), state.primary._fields());
            q.wp("RETURNING").use(state.primary._identity(), rendered::render);
        }
    }

    @Override
    public void delete(Writer q, State state, Rendered rendered) {
        rendered.stage = Stage.DELETE;
        q.w("DELETE").wp("FROM").use(w -> state.primary._render(rendered, w));
        whereBuilder(q, state, rendered, true);
    }

    @Override
    public void softDelete(Writer q, State state, Rendered rendered) {
        var audit = state.primary._auditable();
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
        rendered.render(q, new StmtSet<>(TraitField.REMOVED.of(state.primary), true));
        if (audit) {
            var actor = state.actor;
            var mod = TraitField.MODIFIER.of(state.primary);
            if (actor == null && mod instanceof Value.NumberValue<?>) actor = -1;
            q.w(',');
            rendered.render(q, new StmtSet<>(mod, actor)).w(',');
            rendered.render(q, new StmtSet<>(TraitField.MODIFIED_AT.of(state.primary), RawValue.NOW));
        }
        whereBuilder(q, state, rendered, false);
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
    public Object parameter(Writer w, Object value) {
        return Dialect.super.parameter(w, value);
    }

    @Override
    public Throwable exceptionHandler(Throwable e) {

        if (e instanceof PgException pe) {
            // Handle specific PostgreSQL error codes
            return switch (pe.getSqlState()) {
                // Constraint violations
                case "23505" -> // unique_violation
                        new StoreError(DATA_VIOLATION_DUPLICATED, "Duplicate entry found", pe);
                case "23503" -> // foreign_key_violation
                        new StoreError(DATA_VIOLATION_MISSING_FK, "Foreign key constraint violation", pe);
                case "23502" -> // not_null_violation
                        new StoreError(DATA_VIOLATION_NULL_VALUE, "Not null constraint violation", pe);
                case "23514" -> // check_violation
                        new StoreError(DATA_VIOLATION_CONSTRAINT, "Check constraint violation", pe);

                // Syntax and invalid operations
                case "42601" -> // syntax_error
                        new StoreError(GRAMMAR_SYNTAX, "SQL syntax error", pe);
                case "42883" -> // undefined_function
                        new StoreError(GRAMMAR_FUNCTION, "Undefined function", pe);
                case "42P01" -> // undefined_table
                        new StoreError(GRAMMAR_TABLE, "Undefined table", pe);
                case "42703" -> // undefined_column
                        new StoreError(GRAMMAR_COLUMN, "Undefined column", pe);

                // Data exceptions
                case "22001" -> // string_data_right_truncation
                        new StoreError(DATA_ACCESS_OVERFLOW, "Data too long for column", pe);
                case "22003" -> // numeric_value_out_of_range
                        new StoreError(DATA_ACCESS_RANG, "Numeric value out of range", pe);
                case "22007" -> // invalid_datetime_format
                        new StoreError(DATA_ACCESS_FORMAT, "Invalid date/time format", pe);
                case "22P02" -> // invalid_text_representation
                        new StoreError(DATA_ACCESS_TEXT, "Invalid text representation", pe);

                // Connection and transaction issues
                // sqlclient_unable_to_establish_sqlconnection
                // connection_does_not_exist
                // sqlserver_rejected_establishment_of_sqlconnection
                // connection_failure
                case "08001", "08003", "08004", "08006", "08007" -> // transaction_resolution_unknown
                        new StoreError(CONNECTION_ERROR, "Connection issue: " + pe.getMessage(), pe);

                // Deadlock and lock issues
                case "40P01" -> // deadlock_detected
                        new StoreError(LOCK_DEAD_LOCK, "Deadlock detected", pe);
                case "55P03" -> // lock_not_available
                        new StoreError(LOCK_MISSING_LOCK, "Lock not available", pe);

                // Permission and security issues
                case "42501" -> // insufficient_privilege
                        new StoreError(PERMISSION_AUTHORIZATION, "Insufficient privileges", pe);
                case "28000" -> // invalid_authorization_specification
                        new StoreError(PERMISSION_PRIVILEGES, "Invalid authorization", pe);

                // Resource and system limits
                // insufficient_resources
                // disk_full
                // out_of_memory
                case "53000", "53100", "53200", "53300" -> // too_many_connections
                        new StoreError(RESOURCE_LIMIT, "Resource limit exceeded: " + pe.getMessage(), pe);
                // Serialization failures (for transactions)
                case "40001" -> // serialization_failure
                        new StoreError(CONCURRENCY_ERROR, "Transaction conflict: " + pe.getMessage(), pe);
                // Default case for unhandled SQL states
                default -> new StoreError(OTHER_ERROR, "Uncategorized SQL error", pe);
            };
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
        if (field._alias() == null) w.dblQuote(field._name());
        else w.dblQuote(field._alias());
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
        w.w('(').render(root);
        var model = path.get(path.size() - 1).target();
        if (path.size() == 1 && path.get(0).path() instanceof String s) {
            w.w(switch (model) {
                case BOOLEAN -> "->'" + s + "')::boolean";
                case STRING -> "->>'" + s + "')";
                case NUMERIC -> "->'" + s + "')::numeric";
                case LONG -> "->'" + s + "')::int8";
                case INTEGER -> "->'" + s + "')::int4";
                case ARRAY, OBJECT -> "->'" + s + "')::jsonb";
                default -> throw new IllegalStateException("last json mode should not be unknown.");
            });
        } else {
            // Complex path access - use #> path operator
            w.w("#>'{")
                    .each(path, ',', (x, p) -> {
                        var v = p.path();
                        x.w(v instanceof String s ? Json.encode(s) : v.toString());
                    })
                    .w("}')").w(switch (model) {
                        case BOOLEAN -> "::boolean";
                        case STRING -> "::text";
                        case NUMERIC -> "::numeric";
                        case LONG -> "::int8";
                        case INTEGER -> "::int4";
                        case ARRAY, OBJECT -> "::jsonb";
                        default -> throw new IllegalStateException("last json mode should not be unknown.");
                    });
        }
    }

    @Override
    public void jsonRemove(Writer w, Field<?> left, Object[] path) {
        w
                .dblQuote(left._name())
                .w("= ")
                .dblQuote(left._name())
                .w(" #- '{")
                .apply(x -> jsonPath(x, path))
                .w("}'");
    }

    @Override
    public void jsonMerge(Writer w, Field<?> left, Object[] path, Object value) {
        w
                .dblQuote(left._name())
                .w("= jsonb_set(")
                .dblQuote(left._name())
                .w(",'{")
                .apply(x -> jsonPath(x, path))
                .w("}',")
                .w("(")
                .dblQuote(left._name())
                .w("#>'{")
                .apply(x -> jsonPath(x, path))
                .w("}') || (TO_JSONB(")
                .render(value)
                .w("))")
                .w(", true)");
    }

    @Override
    public void jsonSet(Writer w, Field<?> left, Object[] path, Object value) {
        w
                .dblQuote(left._name())
                .w("= jsonb_set(")
                .dblQuote(left._name())
                .w(",'{")
                .apply(x -> jsonPath(x, path))
                .w("}',TO_JSONB(")
                .render(value)
                .w("), true)"); // true for create if missing
    }

    static void jsonPath(Writer w, Object[] path) {
        w.each(path, ',', (x, p) ->
                x.w(p instanceof String s ? Json.encode(s) : p.toString())
        );
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
                    w.dblQuote(model._alias());
                } else {
                    w.dblQuote(model._name());
                }
            }
            case INSERT, JOIN, FROM, UPDATE, DELETE -> w.when(model._schema(), Writer::dblQuote, Writer::dot)
                    .dblQuote(model._name())
                    .when(model._alias(), Writer::sp, Writer::dblQuote);
            default -> throw new IllegalStateException("unhandled model stage: " + w.rendered().stage);
        }
    }

    @Override
    public void field(Writer w, Field<?> field) {
        switch (w.rendered().stage()) {
            case SELECT -> {
                if (field._owner() != null) {
                    model(w, field._owner());
                    w.dot();
                }
                if (field._alias() == null) w.dblQuote(field._name());
                else w.dblQuote(field._name()).wp("AS").dblQuote(field._alias());
            }
            case COLUMNS -> w.dblQuote(field._name());
            case SET -> {
                if (field._alias() == null) {
                    w.dblQuote(field._name());
                } else
                    w.dblQuote(field._alias());
            }
            case LIMITS,
                 INSERT,

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
                    w.dblQuote(field._name());
                } else
                    w.dblQuote(field._alias());
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
        return PgBuilder.pool().using(vertx).with(options).connectingTo(connection).build();
    }

    @Override
    public Pool make(Vertx vertx, JsonObject connection, PoolOptions options) {
        return PgBuilder.pool().using(vertx).with(options).connectingTo(new PgConnectOptions(connection)).build();
    }
}
