package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public
record Compare(Operator op, Value<?> left, Object right) implements Expr<Boolean>, Value.BooleanValue {
    public Compare(Operator op, Value<?> left) {
        this(op, left, null);
    }

    public Compare(Value<?> left, Object low, Object right) {
        this(Operator.BETWEEN, left, new Object[]{low, right});
    }

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (op) {
            case EQ -> renderer.equal(w,left, right);
            case NON_EQ -> renderer.noneEqual(w,left, right);
            case IS_NULL -> renderer.isNull(w,left);
            case NON_NULL -> renderer.isNotNull(w,left);
            case IS_TRUE -> renderer.isTrue(w,left);
            case IS_FALSE -> renderer.isFalse(w,left);
            case GT -> renderer.greater(w,left, right);
            case GTE -> renderer.greaterOrEqual(w,left, right);
            case LT -> renderer.lesser(w,left, right);
            case LTE -> renderer.lesserOrEqual(w,left, right);
            case IN -> renderer.in(w,left, (Object[]) right);
            case NOT_IN -> renderer.notIn(w,left, (Object[]) right);
            case BETWEEN -> {
                var o = (Object[]) right;
                var l = o[0];
                var h = o[1];
                renderer.between(w,left, l, h);
            }
            case LIKE_CONTAINS -> renderer.contains(w,left, right, false);
            case LIKE_BEGINS -> renderer.startsWith(w,left, right, false);
            case LIKE_ENDS -> renderer.endsWith(w,left, right, false);
            case I_LIKE_CONTAINS -> renderer.contains(w,left, right, true);
            case I_LIKE_BEGINS -> renderer.startsWith(w,left, right, true);
            case I_LIKE_ENDS -> renderer.endsWith(w,left, right, true);
            case I_EQ -> renderer.equalCaseInsensitive(w,left, right);
            case I_NONE_EQ -> renderer.notEqualCaseInsensitive(w,left, right);
            default -> throw new UnsupportedOperationException("Not supported yet: " + op);
        }
    }

    public enum Operator {
        EQ,
        NON_EQ,
        IS_NULL,
        NON_NULL,
        IS_TRUE,
        IS_FALSE,
        GT,
        GTE,
        LT,
        LTE,
        IN,
        NOT_IN,
        BETWEEN,
        LIKE_CONTAINS,
        LIKE_BEGINS,
        LIKE_ENDS,
        I_LIKE_CONTAINS,
        I_LIKE_BEGINS,
        I_LIKE_ENDS,
        I_EQ,
        I_NONE_EQ,
    }
}
