package vat.core.factory;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.CorsHandler;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;
import vat.api.utils.Pointer;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-11-10


public class CORSHandlerFactory implements ComponentFactory<CorsHandler> {
    @Override
    public Class<CorsHandler> target() {
        return CorsHandler.class;
    }

    @Override
    public Future<CorsHandler> make(Vertx vertx,String name, JsonObject scope) {
        return Future.succeededFuture(configure(CorsHandler.create(),name, scope));
    }

    public static CorsHandler configure(CorsHandler cors,String name, @Nullable JsonObject scope) {
        if (scope == null) return cors;
        var conf = Pointer.of("/cors").getObject(scope).orElse(null);
        if (conf == null) return cors;
        //Origins
        Pointer.of("/origins").getString(conf)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(x -> {
                    if (x.indexOf(',') > 0) cors.addOrigins(Arrays.asList(x.split(",")));
                    else cors.addOrigin(x);
                });
        //Methods
        Pointer.of("/allowMethods").getString(conf)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(x -> {
                    if (x.indexOf(',') > 0) cors.allowedMethods(Stream.of
                                    (x.split(","))
                            .filter(Predicate.not(String::isBlank))
                            .map(String::toUpperCase)
                            .map(HttpMethod::valueOf).collect(Collectors.toSet()));
                    else cors.allowedMethod(HttpMethod.valueOf(x.toUpperCase()));
                });
        //Allow Headers
        Pointer.of("/allowHeaders").getString(conf)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(x -> {
                    if (x.indexOf(',') > 0) cors.allowedHeaders(Seq
                            .seq(x.split(","))
                            .filter(Predicate.not(String::isBlank))
                            .toSet());
                    else cors.allowedHeader(x);
                });
        //Expose Header
        Pointer.of("/exposeHeaders").getString(conf)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(x -> {
                    if (x.indexOf(',') > 0) cors.exposedHeaders(Seq
                            .seq(x.split(","))
                            .filter(Predicate.not(String::isBlank))
                            .toSet());
                    else cors.exposedHeader(x);
                });
        //relativeOrigins
        Pointer.of("/regexOrigins").getString(conf)
                .filter(Predicate.not(String::isBlank))
                .ifPresent(x -> {
                    if (x.indexOf(',') > 0) cors.addOriginsWithRegex(Stream.of
                                    (x.split(","))
                            .filter(Predicate.not(String::isBlank))
                            .toList());
                    else cors.addOriginWithRegex(x);
                });
        if (Pointer.of("/allowCredentials").getBoolean(conf).orElse(false)) cors.allowCredentials(true);
        if (Pointer.of("/allowPrivateNetwork").getBoolean(conf).orElse(false)) cors.allowPrivateNetwork(true);
        Pointer.of("/maxAgeSeconds").getInteger(conf)
                .filter(x -> x > 0)
                .ifPresent(cors::maxAgeSeconds);
        return cors;
    }
}
