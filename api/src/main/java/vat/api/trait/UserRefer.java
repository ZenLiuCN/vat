package vat.api.trait;

import vat.api.meta.Describe;

///
/// @author Zen.Liu
/// @since 2025-12-01


public interface UserRefer {
    @Describe(value = "user", desc = "user id that referenced", identity = "vat.foundation.users.api.Users::identity")
    long user();
}
