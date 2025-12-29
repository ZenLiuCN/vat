package vat.api.store;

import org.jspecify.annotations.Nullable;

///
/// @author Zen.Liu
/// @since 2025-10-31

@FunctionalInterface
public interface Interceptor<T extends @Nullable Object> {
    T intercept(boolean read, T t);
}
