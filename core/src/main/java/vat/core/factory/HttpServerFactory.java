package vat.core.factory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import vat.api.DomainError;
import vat.api.utils.Pointer;

///
/// @author Zen.Liu
/// @since 2025-11-10


public class HttpServerFactory implements ComponentFactory<HttpServerFactory.ServerInfo> {
    public   record ServerInfo(HttpServerOptions options,HttpServer server){}
    @Override
    public Class<ServerInfo> target() {
        return ServerInfo.class;
    }

    @Override
    public Future<ServerInfo> make(Vertx vertx, String name, JsonObject scope) {
        return Future.future(p -> {
            var conf = Pointer.of("/http").getObject(scope).orElseThrow(() -> DomainError.System.internalServerError("missing /" + name + "/http config"));
            var nativeTransport = vertx.isNativeTransportEnabled();
            var opt= nativeTransport ? new HttpServerOptions(conf)
                    .setTcpFastOpen(true)
                    .setTcpCork(true)
                    .setTcpQuickAck(true)
                    .setReusePort(true)
                    : new HttpServerOptions(conf);
            p.complete(new ServerInfo(opt,vertx.createHttpServer(opt)));
        });
    }
}
