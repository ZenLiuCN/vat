package vat.codegen.utils;

///
/// @author Zen.Liu
/// @since 2025-12-04

@FunctionalInterface
public interface Flushable {
    void flush(Domain domain);
}
