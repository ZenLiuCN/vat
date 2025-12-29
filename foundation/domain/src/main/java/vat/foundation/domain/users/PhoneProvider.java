package vat.foundation.domain.users;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import vat.api.implement.DomainManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static vat.foundation.domain.users.PasswordProvider.hash;

///
/// @author Zen.Liu
/// @since 2025-11-13

@SuppressWarnings("unused")
@AutoService(CertificateProvider.class)
public class PhoneProvider implements CertificateProvider {
    @Override
    public int kind() {
        return 1;
    }


    @Override
    public Future<Boolean> test(Vertx vertx, DomainManager manager, String identifier, JsonObject raw, JsonObject stored) {
        //TODO
        var sk =stored.getString("secret",""+System.currentTimeMillis());
        var rh = hash(identifier, identifier);
        var sh = hash(sk, identifier);
        return Future.succeededFuture( MessageDigest.isEqual(rh.getBytes(StandardCharsets.UTF_8),sh.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Future<JsonObject> store(Vertx vertx, DomainManager manager, String identifier, JsonObject raw) {
        return vertx.executeBlocking(()-> JsonObject.of("secret", hash(identifier, identifier)));
    }
}
