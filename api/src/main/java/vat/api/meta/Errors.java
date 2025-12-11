package vat.api.meta;

import java.lang.annotation.*;

///
/// Make an Activities's domain context default method as error configuration
/// 1. error builder use to mark a context method is error builder from configuration
/// 2. error builder method should accept none or any amount of arguments
/// 3. error builder method must be annotated with {@link Errors}
/// 4. defaults: error build will read `/error/[camelCaseName]/code` and `/error/[camelCaseName]/message` as alternative parameter.
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Errors {
    /// the config object pointer, default is `/errors/MethodName`
    String value() default "";

}
