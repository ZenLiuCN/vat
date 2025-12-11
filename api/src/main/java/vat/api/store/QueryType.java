package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public enum QueryType {
    SELECT_ONE,
    SELECT_ANY,
    COUNTING,

    UPDATE,
    INSERT,
    INSERT_RETURNS,
    INSERT_MULTI,
    INSERT_MULTI_RETURNS,
    DELETE,


}
