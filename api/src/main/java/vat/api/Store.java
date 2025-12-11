package vat.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// @author Zen.Liu
/// @since 2025-11-01
@Prototype
public interface Store<T> {
    Logger log= LoggerFactory.getLogger(Store.class);
}
