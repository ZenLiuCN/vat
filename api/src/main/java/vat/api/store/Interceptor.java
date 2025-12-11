package vat.api.store;

///
/// @author Zen.Liu
/// @since 2025-10-31

@FunctionalInterface
public interface Interceptor<T> {
    T intercept(boolean read, T t);
}
