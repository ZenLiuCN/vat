package vat.api;

import vat.api.trait.UserRefer;

///
/// @author Zen.Liu
/// @since 2025-10-27

@Prototype
public interface Ability extends Data, Entity {

    interface Base extends Ability, Entity.Base, UserRefer {
    }
}
