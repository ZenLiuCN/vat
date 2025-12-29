package vat.foundation.domain.users;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.jooq.lambda.Sneaky;
import org.jspecify.annotations.Nullable;
import vat.api.implement.DomainManager;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

///
/// @author Zen.Liu
/// @since 2025-11-13

@AutoService(CertificateProvider.class)
public record PasswordProvider() implements CertificateProvider {
    @Override
    public int kind() {
        return 0;
    }

    @Override
    public Future<Boolean> test(Vertx vertx, DomainManager manager, String identifier, JsonObject raw, JsonObject stored) {
        return vertx.executeBlocking(() -> {
            var rk = raw.getString("secret", "");
            var sh = stored.getString("secret", "" + System.currentTimeMillis());
            var rh = hash(rk, identifier);
            return MessageDigest.isEqual(rh.getBytes(StandardCharsets.UTF_8), sh.getBytes(StandardCharsets.UTF_8));
        });
    }

    @Override
    public Future<JsonObject> store(Vertx vertx, DomainManager manager, String identifier, JsonObject raw) {
        return vertx.executeBlocking(() -> JsonObject.of("secret", hash(raw.getString("secret"), identifier)));
    }

    @SneakyThrows
    static String hash(String password, String secret, @Nullable Integer it) {
        var salt = secret.getBytes(StandardCharsets.UTF_8);
        var spec = new PBEKeySpec(password.toCharArray(), salt, it == null ? 10000 : it, 64 * 8);
        return Base64.getEncoder().withoutPadding().encodeToString(skf.generateSecret(spec).getEncoded());
    }

    static String hash(String password, String secret) {
        return hash(password, secret, iteration(password.charAt(0)));
    }

    static String hash(String password, String secret, char rune) {
        return hash(password, secret, iteration(rune));
    }

    static int iteration(char ch) {
        return ch < 10000 ? ch + 10000 : ch; //≈ 4x mills
    }

    static final SecretKeyFactory skf = Sneaky
            .supplier(() -> SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512"))
            .get();
}
