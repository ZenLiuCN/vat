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

public non-sealed interface F2 extends F {
    Activities make(Vertx vertx, @Nullable String address, JsonObject conf) throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;

    @SneakyThrows
    default Activities apply(Vertx vertx, @Nullable String address, JsonObject conf) {
        return make(vertx, address, conf);
    }

    class V2 extends Base<F2> {

        V2(F2 func,String name, JsonObject conf) {
            super(func,name, conf);
        }

        @Override
        Future<Activities> factory(Vertx vertx,String name, JsonObject conf, F2 func) {
            return Future.future(p -> p.complete(func.apply(vertx, Pointer.of("/address").getString(conf).orElse(null), conf)));
        }
    }

    record Impl(int mode, boolean auto, String name, String domain, F2 func) implements ActivityFactory {
        @Override
        public VerticleBase make(JsonObject conf) {
            return new V2(func,domain, Objects.requireNonNull(conf.getJsonObject(domain), () -> "missing activities config for " + domain));
        }
    }
}

