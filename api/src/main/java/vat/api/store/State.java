package vat.api.store;

import io.vertx.sqlclient.SqlClient;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
@Getter
@Accessors(fluent = true)
public  class State {
    public boolean withHistory;
    public final SqlClient sql;
    public final Dialect dialect;

    public final Model<?> primary;
    public @Nullable List<StmtJoin> joined;
    public @Nullable Statement.SetStmt[] sets;
    public @Nullable List<Value<?>> picks;
    public @Nullable StmtOrder[] ordered;
    public @Nullable Field<?>[] grouped;
    public @Nullable Value.BooleanValue having;
    public @Nullable Value.BooleanValue cond;
    public QueryType type;
    public int tuples = 0;
    public Integer skip;
    public Integer limit;
    public boolean permanent;
    public StmtAssign[] assigns;
    public StmtAssign[][] assignsMany;
    /// current operator ID
    public Object actor;

    public boolean hasJoin() {
        return joined != null && !joined.isEmpty();
    }

    public boolean hasPick() {
        return picks != null && !picks.isEmpty();
    }

    public State(Model<?> primary, SqlClient sql, Dialect dialect) {
        this.primary = primary;
        this.sql = sql;
        this.dialect = dialect;
    }

    public void join(StmtJoin stmt) {
        if (joined == null) {
            joined = new ArrayList<>();
        }
        joined.add(stmt);
        tuples = joined.size() + 1;
    }


    public void pick(Value<?>[] field) {
        if (picks == null) {
            picks = new ArrayList<>();
        }
        picks.addAll(Arrays.asList(field));
    }

    public Rendered doRender() {
        return dialect.render(this, new Rendered(dialect));
    }

    public void filter(Value.BooleanValue condition) {
        if (cond == null) cond = condition;
        else cond = cond.and(condition);
    }
}
