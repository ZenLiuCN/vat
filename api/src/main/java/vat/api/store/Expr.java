package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public sealed interface Expr<T> extends Value<T> permits Bitwise, Compare, JsonGet, Logical, Mathematics {

}
