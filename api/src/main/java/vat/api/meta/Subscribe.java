package vat.api.meta;

import java.lang.annotation.*;

/// Make an Activities's domain context default method as Event subscriber.
/// @author Zen.Liu
/// @since 2025-10-27

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Subscribe {
    /// string of address or json pointer to address which starts with `/`.
    String value() default "";
}
