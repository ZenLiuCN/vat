package vat.core.verticles;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import vat.api.Activities;

import static vat.core.BootstrapVerticle.UNDEPLOY_LISTENER;

///
/// @author Zen.Liu
/// @since 2025-11-11


abstract class Base<T extends F> extends VerticleBase {
    private T func;
    private String name;
    private JsonObject conf;

    Base(T func, String name, JsonObject conf) {
        this.func = func;
        this.name = name;
        this.conf = conf;
    }

    private volatile Activities activities;

    abstract Future<Activities> factory(Vertx vertx, String name, JsonObject conf, T func);


    Future<?> launch() {
        return Future.succeededFuture();
    }


    Future<?> halt() {
        return Future.succeededFuture();
    }

    @Override
    public Future<?> start() {
        return factory(vertx, name, conf, func)
                .map(f -> {
                    activities = f;
                    conf = null;//! recycle.
                    func = null;
                    name = null;
                    return null;
                })
                .flatMap($ -> this.launch());
    }

    @Override
    public Future<?> stop() {
        vertx.eventBus().publish(UNDEPLOY_LISTENER, deploymentID(),new DeliveryOptions().setLocalOnly(true));
        return halt().flatMap($ -> activities == null ? Future.succeededFuture() : activities.dispose());
    }
}
