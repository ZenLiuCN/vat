package vat.api;

///
/// @author Zen.Liu
/// @since 2025-10-27

@Prototype
public interface Record extends Data, Entity {
    interface Base extends Record, Entity.Base {
    }
}
