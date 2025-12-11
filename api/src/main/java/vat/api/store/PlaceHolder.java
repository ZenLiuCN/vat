package vat.api.store;

import vat.api.DomainError;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public record PlaceHolder(
        String name, Class<?> type
) implements Renderable {

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
       if(! renderer.registerPlaceHolder(w,name,type)){
           throw DomainError.System.conflict("PlaceHolder exists {}",name);
       }
    }
}
