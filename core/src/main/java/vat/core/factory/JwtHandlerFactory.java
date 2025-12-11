package vat.core.factory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import vat.api.DomainError;
import vat.api.implement.Authenticator;
import vat.api.implement.Codec;
import vat.api.utils.Pointer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-11-10


public class JwtHandlerFactory implements ComponentFactory<Authenticator> {
    @Override
    public Class<Authenticator> target() {
        return Authenticator.class;
    }

    static final String KIND_JWT = "jwt";
    static final String KIND_CUSTOMER = "customer";

    static Authenticator.TokenReader locator(JsonObject conf) {
        var keyName = Pointer.of("/name").getString(conf).orElse("authorization");
        var locations = Pointer.of("/locations")
                .getArray(conf).orElseGet(() -> JsonArray.of("header"))
                .stream()
                .map(x -> x instanceof String s ? s.trim().toLowerCase() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (locations.contains("any")) locations.addAll(List.of("header", "query", "path"));
        Authenticator.TokenReader locator = null;
        if (locations.contains("header")) locator = Authenticator.HEADER.apply(keyName);
        if (locations.contains("query"))
            locator = locator == null ? Authenticator.QUERY.apply(keyName) : locator.orElse(Authenticator.QUERY.apply(keyName));
        if (locations.contains("path"))
            locator = locator == null ? Authenticator.PATH.apply(keyName) : locator.orElse(Authenticator.PATH.apply(keyName));
        return locator == null ? Authenticator.HEADER.apply(keyName) : locator;
    }

    @Override
    public Future<Authenticator> make(Vertx vertx, String name, JsonObject scope) {
        return Future.future(p -> {
            var conf = Pointer.of("/authenticator").getObject(scope)
                    .orElseThrow(() -> DomainError.System.internalServerError("missing /" + name + "/authenticator config"));
            switch (Pointer.of("/kind").getString(conf).map(String::toLowerCase).orElse(null)) {
                case KIND_JWT -> {
                    var opt = new JWTAuthOptions(Pointer.of("/config").getObject(conf).orElseGet(JsonObject::new));
                    byte[] key = null;
                    var oid = Pointer.of("/openId").getString(conf).orElse(null);
                    if (oid != null && !oid.isBlank()) {
                        key = oid.getBytes(StandardCharsets.UTF_8);
                    }
                    var locator = locator(conf);
                    var authority = Pointer.of("/authority").getBoolean(conf).orElse(false);
                    if (key != null && authority) {
                        p.complete(new Authenticator.Generator<>(locator, new Authenticator.OpenIDJwt(vertx, JWTAuth.create(vertx, opt), key, opt)));
                    } else if (key != null) {
                        p.complete(new Authenticator.Validator(locator, new Authenticator.OpenIDJwt(vertx, JWTAuth.create(vertx, opt), key, opt)));
                    } else {
                        p.complete(new Authenticator.Validator(locator, JWTAuth.create(vertx, opt)));
                    }
                }
                case KIND_CUSTOMER -> {
                    var cls = Pointer.of("/class").getString(conf)
                            .map(Codec::clazz)
                            .orElseThrow(() -> DomainError.System.internalServerError("missing /" + name + "/authenticator/class config"));
                    var o = Codec.<Authenticator.Customer>instance(cls);
                    var locator = locator(conf);
                    p.complete(o.initialize(vertx, locator, conf));
                }
                case null, default -> p.fail("missing authenticator config for : "+name);
            }
        });
    }
}
