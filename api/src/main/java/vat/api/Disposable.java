package vat.api;

import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;


/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Prototype
public interface Disposable {
    Future<@Nullable Void> dispose();
}
