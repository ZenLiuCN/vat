package vat.core.component;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import io.vertx.core.Future;
import vat.api.DomainError;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

///
/// @author Zen.Liu
/// @since 2025-11-24


public interface Caches {
    interface AsyncLoader<K, V> extends AsyncCacheLoader<K, V> {
        Future<? extends Map<? extends K, ? extends V>> many(Set<? extends K> keys);

        Future<? extends V> one(K key);

        default Future<? extends V> reload(K key, V old) {
            return one(key);
        }

        @Override
        default CompletableFuture<? extends V> asyncLoad(K key, Executor executor) {
            return one(key).toCompletionStage().toCompletableFuture();
        }

        @Override
        default CompletableFuture<? extends Map<? extends K, ? extends V>> asyncLoadAll(Set<? extends K> keys, Executor executor) {
            return many(keys).toCompletionStage().toCompletableFuture();
        }

        @Override
        default CompletableFuture<? extends V> asyncReload(K key, V oldValue, Executor executor) {
            return reload(key, oldValue).toCompletionStage().toCompletableFuture();
        }
    }

    interface Async<K, V> extends Caches {
        AsyncLoadingCache<K, V> cache();

        default Future<V> one(K key) {
            return Future.fromCompletionStage(cache().get(key).minimalCompletionStage());
        }

        default Future<? extends Map<? extends K, ? extends V>> any(Collection<? extends K> keys) {
            if (keys.size() == 1) return keys.stream()
                    .findFirst()
                    .map(x -> one(x).map(v -> Map.of(x, v)))
                    .orElseThrow(() -> DomainError.System.badRequest("invalid keys for cache {}", this));
            return Future.fromCompletionStage(cache().getAll(keys));
        }
    }
}
