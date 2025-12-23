package vat.api.meta;

import vat.api.implement.Interceptors;
import vat.api.implement.Validators;

import java.lang.annotation.*;

/// A data property interceptor to check or convert value.
/// This should always annotate on getter method.
/// The intercept will invoke at set property or construct
///
/// @author Zen.Liu
/// @since 2025-10-27
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
@Repeatable(Intercept.List.class)
public @interface Intercept {
    /// invoke when constructing, otherwise only invoke at set value
    boolean construct() default false;

    ///  name of static {@link java.util.function.UnaryOperator} field that accept the property value
    String value();

    /// holder interface or class for interceptor field
    Class<?> holder() default Interceptors.class;

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Documented
    @interface List {
        Intercept[] value();
    }
}
