package vat.api;

import io.vertx.core.Future;

/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Prototype
public interface Disposable {
    Future<Void> dispose();
}
