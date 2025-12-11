package vat.api.meta;

import java.lang.annotation.*;

/**
 * json property key override.
 * With alias, original field name accepted as input key. and only alias name accept as write key.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Alias {
    String value();
    /// With strict mode, only alias value accept, drop original field name.
    boolean strict() default true;
}
