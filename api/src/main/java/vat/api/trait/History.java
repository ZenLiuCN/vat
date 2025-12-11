package vat.api.trait;

import io.vertx.core.json.JsonObject;
import vat.api.Data;
import vat.api.meta.Describe;
import vat.api.meta.Historic;
import vat.api.meta.Nullable;

/// Entity with history recording
///
/// @author Zen.Liu
/// @since 2025-11-11

public interface History extends Data {
    @Describe(value = "_HISTORY",desc = "_DESC_HISTORY")
    @Historic
    @Nullable JsonObject history();
}
