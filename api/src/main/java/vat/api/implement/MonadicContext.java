package vat.api.implement;

import io.vertx.core.Future;
import vat.api.Disposable;
import vat.api.Domain;

import java.util.Map;

///
/// @author Zen.Liu
/// @since 2025-12-31


public interface MonadicContext extends Domain.Context, Disposable, DomainManager {
    Map<String, Disposable> registry();
    @Override
    default Future<Void> dispose() {
        return Future.join(registry().values().stream()
                .map(Disposable::dispose)
                .toList()
        ).mapEmpty();
    }
}
