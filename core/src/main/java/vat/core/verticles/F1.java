package vat.core.verticles;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import vat.api.Activities;

import vat.api.utils.Pointer;

import java.util.Objects;

///
/// @author Zen.Liu
/// @since 2025-11-11
public non-sealed interface F1 extends F {
    @SneakyThrows
    default Activities apply(Vertx v, @Nullable String address) {
        return make(v, address);
    }

    Activities make(Vertx v, @Nullable String address)
            throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;

    class V1 extends Base<F1> {
        protected V1(F1 activitiesFunc,String name, JsonObject conf) {
            super(activitiesFunc,name, conf);
        }

        @Override
        protected Future<Activities> factory(Vertx vertx,String name, JsonObject conf, F1 func) {
            return Future.future(p -> p.complete(func.apply(vertx, Pointer.of("/address").getString(conf).orElse(null))));
        }
    }

    record Impl(int mode, boolean auto, String name, String domain, F1 func) implements ActivityFactory {
        @Override
        public VerticleBase make(JsonObject conf) {
            return new V1(func,domain, Objects.requireNonNull(conf.getJsonObject(domain), () -> "missing activities config for " + domain));
        }
    }
}
