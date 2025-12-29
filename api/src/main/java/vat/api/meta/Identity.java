package vat.api.meta;

import vat.api.Activities;
import vat.api.Entity;

import java.lang.annotation.*;

/**
 * Mark a column is Identity
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface Identity {
    /// mark an identity type field reference to an Entity
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Documented
    @interface Reference {
        /// the entity that referenced to
        Class<? extends Entity> value();

        /// provide activities
        Class<? extends Activities> provider() default Activities.class;

        /// provide activity name, valid when {@link #provider()} not {@link Activities}.
        String identity() default "identity";

        /// provide activity domain identity
        String provide() default "";
    }

    /// 1. mark an identity type field reference to an Entity
    /// 2. mark on an Entity for its identity function
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Documented
    @interface Refer {
        Class<? extends Activities> domain() default Activities.class;

        /// full identity Identifier
        String value() default "identity";
    }
}
