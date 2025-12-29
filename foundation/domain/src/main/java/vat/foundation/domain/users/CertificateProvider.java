package vat.foundation.domain.users;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;
import vat.api.implement.DomainManager;
import vat.api.utils.Primitives;

import java.util.ServiceLoader;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface CertificateProvider {
    int kind();

    default String name() {
        return this.getClass().getSimpleName();
    }

    Future<Boolean> test(Vertx vertx, DomainManager manager, String identifier, JsonObject raw, JsonObject stored);

    Future<JsonObject> store(Vertx vertx, DomainManager manager, String identifier, JsonObject raw);

    @SuppressWarnings("Java9UndeclaredServiceUsage")
    IntObjectMap<@Nullable CertificateProvider> PROVIDERS = ServiceLoader
            .load(CertificateProvider.class, CertificateProvider.class.getClassLoader())
            .stream()
            .map(ServiceLoader.Provider::get)
            .distinct()
            .reduce(new IntObjectHashMap<>(), Primitives.accumulator(CertificateProvider::kind), Primitives::combine);
}
