package vat.api;

///
/// @author Zen.Liu
/// @since 2025-10-27

@Prototype
public interface Actor extends Data, Entity {
    interface Base extends Actor, Entity.Base {
    }
}
