package vat.core.factory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import vat.api.DomainError;
import vat.api.store.Dialect;
import vat.api.utils.Pointer;
import vat.core.store.MySQLDialect;
import vat.core.store.PgDialect;

///
/// @author Zen.Liu
/// @since 2025-11-10


public class SqlPoolFactory implements ComponentFactory<SqlPoolFactory.SqlEntry> {
    public record SqlEntry(Pool pool, Dialect dialect) {
    }

    @Override
    public Class<SqlEntry> target() {
        return SqlEntry.class;
    }

    @Override
    public Future<SqlEntry> make(Vertx vertx, String name, JsonObject scope) {
        return Future.future(p -> {
            var conf = Pointer.of("/store").getObject(scope).orElseThrow(() -> DomainError.System.internalServerError("missing /" + name + "/store configuration"));
            var dialectName = Pointer.of("/dialect").getString("conf").orElse("MySQL").toLowerCase();
            var dialect = switch (dialectName) {
                case "mysql" -> new MySQLDialect();
                case "postgres" -> new PgDialect();
                default ->
                        throw DomainError.System.internalServerError("dialect only support MySQL or Postgres, got {}", dialectName);
            };
            dialect.config();//!
            var poolOption = Pointer.of("/poolOptions").getObject(conf)
                    .map(x -> x.put("shared", x.getBoolean("shared", true)))
                    .map(PoolOptions::new)
                    .orElseGet(() -> new PoolOptions().setName(dialect.name()).setShared(true));
            var conn = Pointer.of("/connection").getValue(conf).orElseThrow(() -> DomainError.System.internalServerError("missing /" + name + "/store/connection config"));
            if (conn instanceof String s) {
                p.complete(new SqlEntry(dialect.make(vertx, s, poolOption), dialect));
            } else if (conn instanceof JsonObject s) {
                p.complete(new SqlEntry(dialect.make(vertx, s, poolOption), dialect));
            } else
                throw DomainError.System.internalServerError("invalid /" + name + "/store/connection config type: " + conn);
        });
    }
}
