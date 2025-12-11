package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record StmtAssign(Field<?> left, Object value) implements Statement, FieldAndValue {

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.assign(w, left, value);
    }
}
