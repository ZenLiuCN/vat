package vat.api.meta;

import java.lang.annotation.*;

/// Make an Activities's domain context default method as Storage accessor. the method must match signature of
/// `Store<Entity> name(@Nullable SqlConnection tx)`
/// @author Zen.Liu
/// @since 2025-10-27

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Storage {
    /// schema string or json pointer started with `/`
    String value() default "";
}
