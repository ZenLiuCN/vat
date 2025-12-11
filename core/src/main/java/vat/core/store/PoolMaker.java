package vat.core.store;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

///
/// @author Zen.Liu
/// @since 2025-11-11


public interface PoolMaker {
    Pool make(Vertx vertx, String connection, PoolOptions options);

    Pool make(Vertx vertx, JsonObject connection, PoolOptions options);
}
