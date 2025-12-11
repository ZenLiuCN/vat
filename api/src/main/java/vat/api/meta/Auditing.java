package vat.api.meta;


import java.lang.annotation.*;

/**
 * Mark an activities action should audit.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface Auditing {
    enum Mode {
        /// audit for request
        REQUEST,
        /// audit for response
        RESPONSE,
        /// audit before request and response
        INVOKE,
        /// audit for response failure
        FAILURE
    }

    /// Audit mode, default value is invoke
    Mode mode() default Mode.INVOKE;

    /// Default value is `Activities::Action`.
    String topic() default "";
}
