package vat.api.trait;

import io.vertx.core.json.JsonObject;
import vat.api.meta.Describe;

///
/// @author Zen.Liu
/// @since 2025-12-01


public interface Profile {
    @Describe("profile")
    JsonObject profile();
}
