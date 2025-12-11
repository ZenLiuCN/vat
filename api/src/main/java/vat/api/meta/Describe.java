package vat.api.meta;

import java.lang.annotation.*;

/// Describe a Domain object
///
/// @author Zen.Liu
/// @since 2025-10-26

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT
})
@Documented
public @interface Describe {
    /// description
    String desc() default "";

    /// name
    String value();

    /// domain or Activities when use fill identity name
    Class<?> domain() default void.class;

    /// ID method or full domain identity
    String identity() default "identity";

}
