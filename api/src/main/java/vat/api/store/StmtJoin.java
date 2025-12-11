package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtJoin(Mode mode, Model<?> model, Value.BooleanValue cond) implements Statement {
    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (mode) {
            case INNER -> renderer.innerJoin(w,model, cond);
            case LEFT_OUTER -> renderer.leftOuterJoin(w,model, cond);
            case RIGHT_OUTER -> renderer.rightOuterJoin(w,model, cond);
            default -> throw new UnsupportedOperationException("mode not supported yet:" + mode);
        }
    }

    public StmtJoin withAlias(String alias) {
        return new StmtJoin(mode,model._alias(alias),cond);
    }

    public enum Mode {
        INNER, LEFT_OUTER, RIGHT_OUTER
    }
}
