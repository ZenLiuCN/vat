package vat.api.store;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
@Getter
@Accessors(fluent = true)
non-sealed public abstract class Bitwise<T extends Number> implements Expr<T> {
    public final Operator op;
    public final Value<?> left;
    @Nullable public final Object right;
    public final int bits;

    protected Bitwise(Operator op, Value<?> left, @Nullable Object right, int bits) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.bits = bits;
    }

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        switch (op) {
            case SHR -> {
                assert right != null;
                renderer.bitwiseShiftRight(w, left, right, bits);
            }
            case SHL -> {
                assert right != null;
                renderer.bitwiseShiftLeft(w, left, right, bits);
            }
            case AND -> {
                assert right != null;
                renderer.bitwiseAnd(w, left, right, bits);
            }
            case OR -> {
                assert right != null;
                renderer.bitwiseOr(w, left, right, bits);
            }
            case XOR -> {
                assert right != null;
                renderer.bitwiseXor(w, left, right, bits);
            }
            case NOT -> renderer.bitwiseNot(w, left, bits);
            default -> throw new UnsupportedOperationException("Not supported yet: " + op);
        }
    }

    public enum Operator {
        SHR, SHL, AND, OR, XOR, NOT
    }
}
