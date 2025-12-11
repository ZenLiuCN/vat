package vat.core.factory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import vat.api.DomainError;
import vat.api.meta.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface ComponentFactory<T> {
    Class<T> target();

    default Class<?>[] parameters() {
        return new Class[0];
    }

    @Nullable
    Future<T> make(Vertx vertx, String name, @Nullable JsonObject scope);

    @SuppressWarnings("unchecked")
    static <T> Future<T> make(Vertx vertx, Class<T> type, String name, @Nullable JsonObject conf, @Nullable Supplier<T> def, Class<?>... typeArguments) {
        return vertx.executeBlocking(() -> ServiceLoader.load(ComponentFactory.class, vertx.getClass().getClassLoader())
                        .stream()
                        .map(ServiceLoader.Provider::get)
                        .filter(x -> x.target().getCanonicalName().equals(type.getCanonicalName()) &&
                                     (x.parameters().length == 0 || Arrays.equals(x.parameters(), typeArguments)))
                        .findFirst())
                .flatMap(x -> {
                    if (x.isEmpty()) {
                        if (def != null)
                            return Future.succeededFuture(def.get());
                        else
                            return Future.failedFuture(DomainError.System.notFound("Factory of {} {} not found", type, typeArguments));
                    }
                    var f = x.get().make(vertx, name, conf);
                    return Objects.requireNonNullElseGet(f, () -> Future.failedFuture(DomainError.System.notFound("Factory of {} {} not found", type, typeArguments)));
                });
    }

    @SuppressWarnings("unchecked")
    static <T> Future<List<T>> makeAll(Vertx vertx, Class<T> type, String name, @Nullable JsonObject conf, Class<?>... typeArguments) {
        return vertx.executeBlocking(() -> ServiceLoader.load(ComponentFactory.class, vertx.getClass().getClassLoader())
                        .stream()
                        .map(ServiceLoader.Provider::get)
                        .filter(x ->
                                x.target().getCanonicalName().equals(type.getCanonicalName()) &&
                                (x.parameters().length == 0 || Arrays.equals(x.parameters(), typeArguments))
                        ).toList())
                .flatMap(x -> {
                    if (x.isEmpty()) {
                        return Future.succeededFuture(List.of());
                    }
                    return Future.all(x.stream().map(p -> (Future<T>) p.make(vertx, name, conf)).filter(Objects::nonNull).toList())
                            .map(CompositeFuture::list);
                });
    }

}
