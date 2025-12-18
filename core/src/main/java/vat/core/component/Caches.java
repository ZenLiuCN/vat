package vat.core.component;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vat.api.DomainError;
import vat.api.utils.Executors;
import vat.api.utils.Fn;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-11-24


public interface Caches {
    interface AsyncLoader<K, V> extends AsyncCacheLoader<@NotNull K, V> {
        Future<? extends Map<? extends K, ? extends V>> many(Set<? extends K> keys);

        Future<? extends V> one(K key);

        /// Reload method default implement assumes that loading one value cost is higher to check version and loading a value.So just directly load a value.
        default Future<? extends V> reload(K key, V old) {
            return one(key);
        }

        ///  Just convert Future to Caffeine required completable future
        @Override
        default @NotNull CompletableFuture<? extends V> asyncLoad(K key, @NotNull Executor executor) {
            return one(key).toCompletionStage().toCompletableFuture();
        }

        ///  Just convert Future to Caffeine required completable future
        @Override
        default @NotNull CompletableFuture<? extends Map<? extends K, ? extends V>> asyncLoadAll(@NotNull Set<? extends K> keys, @NotNull Executor executor) {
            return many(keys).toCompletionStage().toCompletableFuture();
        }

        ///  Just convert Future to Caffeine required completable future
        @Override
        default @NotNull CompletableFuture<? extends V> asyncReload(K key, @NotNull V oldValue, @NotNull Executor executor) {
            return reload(key, oldValue).toCompletionStage().toCompletableFuture();
        }

        static <K, V> AsyncLoader<K, V> of(
                Function<? super K, Future<? extends V>> one,
                Function<? super Set<? extends K>, Future<? extends Map<? extends K, ? extends V>>> many) {
            return new Impl<>(one, many);
        }

        static <K, V> AsyncLoader<K, V> ofOne(
                Function<? super K, Future<? extends V>> one,
                Function<? super V, ? extends K> classifier) {
            return new Impl<>(one, (k) -> Fn.Many.Flat
                    .all(k, one::apply)
                    .map(Fn.Many.toMap(classifier))
            );
        }

        record Impl<K, V>(
                Function<? super K, Future<? extends V>> one,
                Function<? super Set<? extends K>, Future<? extends Map<? extends K, ? extends V>>> many
        ) implements AsyncLoader<K, V> {

            @Override
            public Future<? extends Map<? extends K, ? extends V>> many(Set<? extends K> keys) {
                return many.apply(keys);
            }

            @Override
            public Future<? extends V> one(K key) {
                return one.apply(key);
            }
        }
    }

    /// Async Loading Cache wrapper.
    interface Async<K, V> extends Caches {
        record Impl<K, V>(AsyncLoadingCache<@NotNull K, V> cache) implements Async<K, V> {
        }

        static <K, V> Impl<K, V> ofContext(Vertx vertx, String spec, AsyncLoader<K, V> loader) {
            return new Impl<>(Caffeine.from(spec).executor(Executors.ofVertxContext(vertx)).buildAsync(loader));
        }

        static <K, V> Impl<K, V> ofWorkers(Vertx vertx, String spec, AsyncLoader<K, V> loader) {
            return new Impl<>(Caffeine.from(spec).executor(Executors.ofVertxWorkers(vertx)).buildAsync(loader));
        }

        AsyncLoadingCache<@NotNull K, V> cache();

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

    /// Indexed cache.
    ///
    /// This cache with two extra index mode:
    /// 1. single index which Key should only locate with one value or none.
    /// 2. multiple index which Key locate with none or one or more value.
    ///
    /// The two index isolated with each other.
    ///
    /// @apiNote index of same mode are shared with one internal Map,should careful with its hashcode.
    sealed interface IndexedAsync<K, V> extends Async<K, V> {
        /// load one by key
        default Future<Optional<V>> byKey(K k) {
            return one(k).map(Optional::ofNullable);
        }

        /// load many by keys (distinct).
        default Future<List<V>> byKeys(Collection<K> keys) {
            return any(new HashSet<>(keys)).map(Map::values).map(ArrayList::new);
        }

        /// invalidate index by index key
        void invalidateSingleOnly(Object... indexKeys);

        /// invalidate index and value by index key
        void invalidateSingle(Object... indexKeys);

        /// invalidate indexes by indexes keys
        void invalidateMultiOnly(Object... indexKeys);

        /// invalidate indexes and values by indexes keys
        void invalidateMulti(Object... indexKeys);

        /// invalidate value by key and relative indexes.
        void invalidate(K key);

        /// only invalidate cached value.
        void invalidateValue(K key);

        /// only find in cached index.
        <T> Future<Optional<V>> single(T indexKey);

        /// only find in cached indexes
        <T> Future<List<V>> multi(T indexesKey);

        /// lookup cached or loading one value by index of T.
        ///
        /// @apiNote This use single index cache map.
        <T> Future<Optional<V>> lookupSingle(T indexKey, Function<T, Future<Optional<V>>> lookup, @Nullable BiConsumer<V, Map<Object, K>> computeIndex);

        /// lookup cached or loading one value by index of T.
        ///
        /// **This method use for a Index Key allocated with only one or none value**
        ///
        /// @apiNote This use single index cache map.
        default <T> Future<Optional<V>> lookupSingle(T indexKey, Function<T, Future<Optional<V>>> lookup) {
            return lookupSingle(indexKey, lookup, null);
        }


        /// lookups cached or loading many value by index of T.
        ///
        /// **This method use for a Index Key allocated with only one or none value**
        ///
        /// @apiNote This use indexes cache map.
        <T> Future<List<V>> lookupMulti(T indexesKey,
                                        Function<T, ? extends Future<? extends List<? extends V>>> lookup,
                                        @Nullable BiConsumer<List<V>, Map<Object, LinkedList<K>>> computeIndexes);

        /// lookups cached or loading many value by index of T.
        ///
        /// **This method use for a Indexes Key allocated with Many values**
        ///
        /// @apiNote This use multiple index cache map.
        default <T> Future<List<V>> lookupMulti(T indexesKey,
                                                Function<T, ? extends Future<? extends List<? extends V>>> lookup) {
            return lookupMulti(indexesKey, lookup, null);
        }

        void clearAllIndex();

        @Accessors(fluent = true)
        non-sealed abstract class Base<K, V> implements IndexedAsync<K, V> {
            @Getter
            protected final AsyncLoadingCache<@NotNull K, V> cache;
            protected final Function<V, K> classifier;
            public final K emptyKey;
            protected final Map<Object, K> index;
            @Getter
            protected final Map<Object, LinkedList<K>> indexes;

            protected Base(Caffeine<@NotNull Object, @NotNull Object> cache,
                           AsyncLoader<K, V> loader,
                           Function<V, K> classifier,
                           K emptyKey,
                           Map<Object, K> indexMap,
                           Map<Object, LinkedList<K>> indexesMap
            ) {
                this.classifier = classifier;
                this.emptyKey = emptyKey;
                this.cache = cache.buildAsync(loader);
                this.indexes = indexesMap;
                this.index = indexMap;
            }

            protected Base(
                    Caffeine<@NotNull Object, @NotNull Object> cache,
                    AsyncLoader<K, V> loader,
                    Function<V, K> classifier,
                    K emptyKey) {
                this.classifier = classifier;
                this.emptyKey = emptyKey;
                this.cache = cache.buildAsync(loader);
                this.indexes = new ConcurrentHashMap<>();
                this.index = new ConcurrentHashMap<>();
            }

            @Override
            public <T> Future<Optional<V>> lookupSingle(
                    T indexKey,
                    Function<T, Future<Optional<V>>> lookup,
                    @Nullable BiConsumer<V, Map<Object, K>> computeIndex) {
                var id = index.get(indexKey);
                if (id == null) {
                    return lookup.apply(indexKey)
                            .map(Fn.Maybe.ifPresent(v -> {
                                var i = classifier.apply(v);
                                cache.synchronous().put(i, v);
                                index.put(indexKey, i);
                                if (computeIndex != null) computeIndex.accept(v, index);
                            }, () -> index.put(indexKey, emptyKey)));
                }
                if (id.equals(emptyKey)) {
                    return Future.succeededFuture(Optional.empty());
                }
                return byKey(id);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Future<List<V>> lookupMulti(
                    T indexKey,
                    Function<T, ? extends Future<? extends List<? extends V>>> lookup,
                    @Nullable BiConsumer<List<V>, Map<Object, LinkedList<K>>> computeIndexes) {
                var id = indexes.get(indexKey);
                //! found
                if (id != null && !id.isEmpty()) {
                    return byKeys(id.stream().filter(Predicate.not(emptyKey::equals)).toList());
                }
                return lookup.apply(indexKey)
                        .map(v -> (List<V>) v)
                        .map(Fn.peek(v -> {
                            var i = v.stream().map(x -> {
                                var ID = classifier.apply(x);
                                cache.synchronous().put(ID, x);
                                return ID;
                            }).distinct().collect(Collectors.toCollection(LinkedList::new));
                            indexes.put(indexKey, i);
                            if (computeIndexes != null) computeIndexes.accept(v, indexes);
                        }));
            }

            @Override
            public <T> Future<Optional<V>> single(T indexKey) {
                return Optional.ofNullable(index.get(indexKey))
                        .filter(Predicate.not(emptyKey::equals))
                        .map(this::byKey)
                        .orElseGet(() -> Future.succeededFuture(Optional.empty()));
            }

            @Override
            public <T> Future<List<V>> multi(T indexKey) {
                return Optional.ofNullable(indexes.get(indexKey))
                        .filter(Predicate.not(List::isEmpty))
                        .map(this::byKeys)
                        .orElseGet(() -> Future.succeededFuture(List.of()));
            }

            @Override
            public void invalidateSingleOnly(Object... indexKeys) {
                for (var key : indexKeys) {
                    index.remove(key);
                }
            }

            @Override
            public void invalidateSingle(Object... indexKeys) {
                var set = new HashSet<K>();
                for (var key : indexKeys) {
                    var x = index.remove(key);
                    if (x != null) set.add(x);
                }
                cache.synchronous().invalidateAll(set);
            }

            @Override
            public void invalidateMultiOnly(Object... indexKeys) {
                for (var key : indexKeys) {
                    indexes.remove(key);
                }
            }

            @Override
            public void invalidateMulti(Object... indexKeys) {
                var set = new HashSet<K>();
                for (var key : indexKeys) {
                    var x = indexes.remove(key);
                    if (x != null) set.addAll(x);
                }
                cache.synchronous().invalidateAll(set);
            }

            @Override
            public void invalidate(K key) {
                index.values().removeIf(v -> v.equals(key));
                indexes.values().removeIf(v -> v.contains(key));
                cache.synchronous().invalidate(key);
            }

            @Override
            public void invalidateValue(K key) {
                cache.synchronous().invalidate(key);
            }

            @Override
            public void clearAllIndex() {
                indexes.clear();
                index.clear();
            }
        }
    }
}
