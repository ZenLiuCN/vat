package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtJsonSet(Operator op, Field<?> left, Object[] path, Object value) implements Statement.SetStmt {
    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (op) {
            case SET -> renderer.jsonSet(w,left, path, value);
            case MERGE -> renderer.jsonMerge(w,left, path, value);
            case REMOVE -> renderer.jsonRemove(w,left, path);
            default -> throw new UnsupportedOperationException("mode not supported yet:" + op);
        }
    }

    StmtJsonSet(Field<?> left, Object[] path) {
        this(Operator.REMOVE, left, path, null);
    }

    public enum Operator {
        SET, MERGE, REMOVE
    }
}
