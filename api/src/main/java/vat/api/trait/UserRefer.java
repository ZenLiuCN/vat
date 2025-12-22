package vat.api.trait;

import vat.api.meta.Describe;
import vat.api.meta.Identity;

///
/// @author Zen.Liu
/// @since 2025-12-01


public interface UserRefer {
    @Describe(value = "user", desc = "user id that referenced")
    @Identity.Refer("vat.foundation.users.api.Users::identity")
    long user();
}
