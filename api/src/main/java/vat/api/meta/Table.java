package vat.api.meta;

import java.lang.annotation.*;

/**
 * table name rewriter
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Table {
    String value();
}
