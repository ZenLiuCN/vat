package vat.api.meta;


import java.lang.annotation.*;

/**
 * Mark a boolean column is soft removed marker
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface SoftRemoved {

}
