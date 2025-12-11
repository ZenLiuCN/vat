package vat.api;

import java.lang.annotation.*;

/// mark a type as protoType.
///
/// @author Zen.Liu
/// @since 2025-10-27

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Prototype {
}
