package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public
record Logical(Operator op, Value<?> left, Object right) implements Expr<Boolean>, Value.BooleanValue {
    Logical( Value<?> left){this(Operator.NOT,left,null);}

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (op){
            case NOT -> renderer.logicalNot(w,left);
            case AND->renderer.logicalAnd(w,left,right);
            case OR -> renderer.logicalOr(w,left,right);
            default -> throw new UnsupportedOperationException("Not supported yet: " + op);
        }
    }

    public enum Operator {
        NOT,
        AND,
        OR,
    }
}
