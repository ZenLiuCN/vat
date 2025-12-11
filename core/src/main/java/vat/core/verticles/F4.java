package vat.core.verticles;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import vat.api.Activities;
import vat.api.meta.Nullable;
import vat.api.store.Dialect;
import vat.api.utils.Pointer;
import vat.core.factory.SqlPoolFactory;

import java.util.Objects;

public non-sealed interface F4 extends F {
    Activities make(Vertx vertx, @Nullable String address, Pool sql, Dialect dialect, JsonObject conf)
            throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;

    @SneakyThrows
    default Activities apply(Vertx vertx,@Nullable String address, Pool sql, Dialect dialect, JsonObject conf) {
        return make(vertx,address, sql, dialect, conf);
    }

    class V4 extends Base<F4> {
        V4(F4 activitiesFunc,String name, JsonObject conf) {
            super(activitiesFunc,name, conf);
        }

        @Override
        protected Future<Activities> factory(Vertx vertx,String name, JsonObject conf, F4 func) {
            return Future.succeededFuture()
                    .flatMap($ -> new SqlPoolFactory().make(vertx,name, conf))
                    .map(x -> func.apply(vertx, Pointer.of("/address").getString(conf).orElse(null), x.pool(), x.dialect(), conf));
        }
    }

    record Impl(int mode, boolean auto, String name, String domain, F4 func) implements ActivityFactory {
        @Override
        public VerticleBase make(JsonObject conf) {
            return new V4(func,domain, Objects.requireNonNull(conf.getJsonObject(domain), () -> "missing activities config for " + domain));
        }
    }
}
