package vat.api.store;

import org.jspecify.annotations.Nullable;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtSet<T>(Field<T> left, @Nullable Object value) implements Statement.SetStmt {

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.set(w, left, value);
    }
}
