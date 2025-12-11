package vat.api.store;

import java.util.function.BiConsumer;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public interface Renderable {
    void _render(Renderer renderer, Writer w);

    static Renderable render(BiConsumer<Renderer, Writer> act) {
        return new Raw(act);
    }

    record Raw(BiConsumer<Renderer, Writer> action) implements Renderable {

        @Override
        public void _render(Renderer renderer, Writer w) {
            action.accept(renderer, w);
        }
    }
}
