package vat.api.meta;

import java.lang.annotation.*;

/**
 * Code generate marker
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Enhance {
    ///  generate a record implement instead of class implement
    ///
    /// @apiNote only for {@link  vat.api.Data}
    boolean record() default true;

    /// also generate a pojo implement.
    ///
    /// @apiNote only for {@link  vat.api.Data}
    boolean pojo() default false;
    /// domain use endpoint model,which make a Domain with Web.Factory parameter
    ///
    /// @apiNote only for {@link vat.api.Activities}
    boolean endpoint() default false;
    /// internal use only marker
    boolean internal() default false;
}
