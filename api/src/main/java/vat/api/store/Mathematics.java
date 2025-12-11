package vat.api.store;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
@Getter
@Accessors(fluent = true)
public non-sealed class Mathematics<T extends Number> implements Expr<T>, Value.NumberValue<T> {
    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (op) {
            case PLUS -> renderer.mathPlus(w,left, right, bits);
            case MINUS -> renderer.mathMinus(w,left, right, bits);
            case TIMES -> renderer.mathTimes(w,left, right, bits);
            case DIVIDE -> renderer.mathDivide(w,left, right, bits);
            case REMINDER -> renderer.mathReminder(w,left, right, bits);
            case NEGATIVE -> renderer.mathNegative(w,left, bits);
            default -> throw new UnsupportedOperationException("Not supported yet: " + op);
        }
    }

    public final Operator op;
    public final Value<?> left;
    public final Object right;
    /// negative for float pointing, 1 or -1 for none normal bits integer or decimal
    public final int bits;

    public Mathematics(Operator op, Value<?> left, Object right, int bits) {
        this.op = op;
        this.left = left;
        this.right = right;

        this.bits = bits;
    }

    public Mathematics(Value<?> left, int bits) {
        this(Operator.NEGATIVE, left, null, bits);
    }


    public enum Operator {
        PLUS, MINUS, TIMES, DIVIDE, REMINDER, NEGATIVE,
    }
}
