package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtSet<T>(Field<T> left, Object value) implements Statement.SetStmt{

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.set(w, left, value);
    }
}
