package vat.api.meta;

import java.lang.annotation.*;

/**
 * Make an Activities's domain context default method as use external domain activities.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Uses {
    /// activity address instead of default.
    /// + a json pointer which starts with `/`, to read from config, in this case default use standard identity.
    /// + an address string which must delimited by `.`.
    String value() default "";

}
