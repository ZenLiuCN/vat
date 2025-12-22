package vat.api.meta;

import java.lang.annotation.*;

/**
 * a virtual field stored in other JsonObject field.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Virtual {
    /// the json object field that stored the value.
    String value();

    String key() default "";
}
