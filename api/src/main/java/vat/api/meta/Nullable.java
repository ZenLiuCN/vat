package vat.api.meta;

import java.lang.annotation.*;

/// alternative of {@link org.jetbrains.annotations.Nullable}, mark for nullable parameters
///
/// @author Zen.Liu
/// @since 2025-11-04

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD
        , ElementType.FIELD
        , ElementType.PARAMETER
        , ElementType.TYPE_USE
        , ElementType.TYPE_PARAMETER
        , ElementType.RECORD_COMPONENT
})
@Documented
public @interface Nullable {
}
