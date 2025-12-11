package vat.api.meta;

import java.lang.annotation.*;

/**
 * Make an Activities's domain context default method as configuration reading.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Config {
    /// config value pointer, default is `/methodName`
    String value() default "";

    /// mapping field name, which should be {@link java.util.function.Function} of `JsonValue=>T`
    String mapping() default "";

    /// mapping function holder
    Class<?> holder() default void.class;

    /// value only use once.
    boolean once() default false;
}
