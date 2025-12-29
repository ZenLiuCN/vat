package vat.api.store;

import org.jspecify.annotations.Nullable;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtAssign(Field<?> left, @Nullable Object value) implements Statement, FieldAndValue {

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.assign(w, left, value);
    }
}
