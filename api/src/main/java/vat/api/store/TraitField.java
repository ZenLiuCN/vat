package vat.api.store;

import org.jetbrains.annotations.Nullable;

///
/// @author Zen.Liu
/// @since 2025-10-28
public enum TraitField {
    VERSION,
    REMOVED,
    CREATOR,
    CREATE_AT,
    MODIFIER,
    MODIFIED_AT,
    HISTORY;

    public <F extends Field<?>> @Nullable F of(Model<?> m) {
        return m._traits(this);
    }
}
