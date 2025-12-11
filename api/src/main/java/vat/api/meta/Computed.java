package vat.api.meta;

import java.lang.annotation.*;

/**
 * marker a  default method for computed value when write to json.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Computed {

}
