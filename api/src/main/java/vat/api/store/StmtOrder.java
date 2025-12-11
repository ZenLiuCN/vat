package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtOrder(boolean desc, Field<?> field) implements Statement {

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.order(w,field,desc);
    }
}
