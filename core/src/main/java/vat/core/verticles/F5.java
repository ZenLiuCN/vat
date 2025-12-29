package vat.core.verticles;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import vat.api.Activities;
import vat.api.implement.Web;
import vat.api.store.Dialect;
import vat.api.utils.Fn;
import vat.api.utils.Pointer;
import vat.core.factory.CORSHandlerFactory;
import vat.core.factory.HttpServerFactory;
import vat.core.factory.JwtHandlerFactory;
import vat.core.factory.SqlPoolFactory;

import java.util.Objects;


///
/// @author Zen.Liu
/// @since 2025-11-11
public non-sealed interface F5 extends F {
    Activities make(Vertx vertx, @Nullable String address, Web.Factory web, Pool sql, Dialect dialect, JsonObject conf) throws java.lang.InstantiationException, java.lang.IllegalAccessException, java.lang.IllegalArgumentException, java.lang.reflect.InvocationTargetException;

    @SneakyThrows
    default Activities apply(Vertx vertx, @Nullable String address, Web.Factory web, Pool sql, Dialect dialect, JsonObject conf) {
        return make(vertx, address, web, sql, dialect, conf);
    }

    class V5 extends Base<F5> {
        protected V5(F5 activitiesFunc, String name, JsonObject conf) {
            super(activitiesFunc, name, conf);
        }

        @Override
        protected Future<Activities> factory(Vertx vertx, String name, JsonObject conf, F5 func) {
            return Future.succeededFuture()
                    .flatMap($ -> Fn.Many.Flat.join(
                            new HttpServerFactory().make(vertx, name, conf),
                            conf.getJsonObject("authenticator") != null ? new JwtHandlerFactory().make(vertx, name, conf) : Future.succeededFuture(),
                            conf.getJsonObject("cors") != null ? new CORSHandlerFactory().make(vertx, name, conf) : Future.succeededFuture(),
                            new SqlPoolFactory().make(vertx, name, conf)
                    ))
                    .map(x -> {
                        var info =  x.v1;
                        server = info.server();
                        server.exceptionHandler(exceptionHandler);
                        var an = x.v2;
                        var cors = x.v3;
                        var entry = x.v4;
                        var router = Router.router(vertx);
                        if (cors != null) router.route().handler(cors);
                        Web.register(router);
                        server.requestHandler(router);
                        var web = Web.of(router, conf, an);
                        Web.log.info("{} endpoints listen {}:{}",name, info.options().getHost(), info.options().getPort());
                        return func.apply(vertx, Pointer.of("/address").getString(conf).orElse(null), web, entry.pool(), entry.dialect(), conf);
                    });

        }

        private HttpServer server;

        protected final Handler<Throwable> exceptionHandler = ex -> Web.log.error("http server process ", ex);

        @Override
        Future<?> launch() {
            return server.listen();
        }

        @Override
        Future<?> halt() {
            return (server == null
                    ? Future.succeededFuture()
                    : server.shutdown())
                    ;

        }
    }

    record Impl(int mode, boolean auto, String name, String domain, F5 func) implements ActivityFactory {
        @Override
        public VerticleBase make(JsonObject conf) {
            return new V5(func, domain, Objects.requireNonNull(conf.getJsonObject(domain), () -> "missing activities config for " + domain));
        }
    }
}
