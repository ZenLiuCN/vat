package vat.api.meta;

import java.lang.annotation.*;

/**
 * Verticle Entry
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Activity {
    enum Mode {
        FOUNDATION,
        COMPONENT,
        DOMAIN,
        ENDPOINT
    }


    Mode mode();

    /// override mode order for deploy
    int order() default -1;

    /// auto reload when config changes
    boolean auto() default false;
}
