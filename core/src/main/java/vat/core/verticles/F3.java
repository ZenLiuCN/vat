package vat.core.verticles;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.SneakyThrows;
import vat.api.Activities;
import vat.api.implement.Web;
import vat.api.meta.Nullable;
import vat.api.utils.Fn;
import vat.api.utils.Pointer;
import vat.core.factory.CORSHandlerFactory;
import vat.core.factory.HttpServerFactory;
import vat.core.factory.JwtHandlerFactory;

import java.util.Objects;

public non-sealed interface F3 extends F {

    Activities make(Vertx vertx, @Nullable String address, Web.Factory web, JsonObject conf)
            throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;

    @SneakyThrows
    default Activities apply(Vertx vertx, @Nullable String address, Web.Factory web, JsonObject conf) {
        return make(vertx, address, web, conf);
    }

    class V3 extends Base<F3> {

        V3(F3 func, String name, JsonObject conf) {
            super(func, name, conf);
        }

        private HttpServer server;

        protected final Handler<Throwable> exceptionHandler = ex -> Web.log.error("http server process ", ex);

        @Override
        Future<Activities> factory(Vertx vertx, String name, JsonObject conf, F3 func) {
            return Future.succeededFuture()
                    .flatMap($ -> Fn.Many.Flat.join(
                            new HttpServerFactory().make(vertx, name, conf),
                            conf.getJsonObject("authenticator") != null ? new JwtHandlerFactory().make(vertx, name, conf) : Future.succeededFuture(),
                            conf.getJsonObject("cors") != null ? new CORSHandlerFactory().make(vertx, name, conf) : Future.succeededFuture()
                    ))
                    .map(x -> {
                        var info = x.v1;
                        server = info.server();
                        server.exceptionHandler(exceptionHandler);
                        var an = x.v2;
                        var cors = x.v3;
                        var router = Router.router(vertx);
                        if (cors != null) router.route().handler(cors);
                        Web.register(router);
                        server.requestHandler(router);
                        var web = Web.of(router, conf, an);
                        Web.log.info("{} endpoints listen {}:{}", name, info.options().getHost(), info.options().getPort());
                        return func.apply(vertx, Pointer.of("/address").getString(conf).orElse(null), web, conf);
                    })
                    ;
        }

        @Override
        Future<?> launch() {
            return server.listen();
        }

        @Override
        Future<?> halt() {
            return (server == null ? Future.succeededFuture() : server.shutdown())
                    ;

        }

    }

    record Impl(int mode, boolean auto, String name, String domain, F3 func) implements ActivityFactory {
        @Override
        public VerticleBase make(JsonObject conf) {
            return new V3(func, domain, Objects.requireNonNull(conf.getJsonObject(domain), () -> "missing activities config for " + domain));
        }
    }
}
