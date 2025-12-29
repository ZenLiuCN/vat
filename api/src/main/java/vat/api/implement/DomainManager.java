package vat.api.implement;

import io.vertx.core.Vertx;
import org.jspecify.annotations.Nullable;
import vat.api.Activities;
import vat.api.Disposable;
import vat.api.Domain;
import vat.api.DomainError;


import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface DomainManager {
    Vertx vertx();

    Map<String, Disposable> registry();

    default <T extends Activities> T activities(Class<T> cls, @Nullable String address) {
        assert cls.isInterface() : "must the domain activities define interface";
        var domain = address == null ? cls.getCanonicalName() : address;
        return Optional.ofNullable(registry().get(domain))
                .or(() -> {
                    var act = Codec.activity(cls).apply(vertx(), domain, null);
                    registry().put(domain, act);
                    return Optional.of(act);
                })
                .filter(cls::isInstance)
                .map(cls::cast)
                .orElseThrow(() -> DomainError.System.conflict("missing domain activities {} ({})", cls, domain))
                ;
    }

    static <T extends Activities> T use(Vertx vertx, Class<T> cls, @Nullable String address) {
        assert cls.isInterface() : "must the domain activities define interface";
        var domain = address == null ? cls.getCanonicalName() : address;
        return Optional.of(Codec.activity(cls).apply(vertx, domain, null))
                .filter(cls::isInstance)
                .orElseThrow(() -> DomainError.System.conflict("missing domain activities {} ({})", cls, domain))
                ;
    }

    default <T extends Activities> T activities(Class<T> cls) {
        return activities(cls, null);
    }

    private static boolean isContext(Class<?> type) {
        return Domain.Context.class.isAssignableFrom(type);
    }

    private static boolean isActivities(Class<?> type) {
        return Activities.class.isAssignableFrom(type);
    }

    static String domain(Class<? extends Activities> t) {
        var faces = t.getSuperclass().getInterfaces();
        if (faces.length == 0) return t.getSimpleName();
        var face = faces[0];//! first interface is the domain face
        if (isContext(face)) {
            face = Arrays.stream(face.getInterfaces())
                    .filter(Predicate.not(DomainManager::isContext).and(DomainManager::isActivities))
                    .findFirst().orElseThrow(() -> DomainError.System.conflict("not found Activities for {}", t));
        }
        return face.getSimpleName();
    }
}
