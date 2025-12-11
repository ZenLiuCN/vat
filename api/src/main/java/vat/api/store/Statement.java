package vat.api.store;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public interface Statement extends Renderable{

    interface SetStmt extends Statement,FieldAndValue {}
}
