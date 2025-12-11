package vat.api.meta;

import java.lang.annotation.*;

/**
 * column rewriter
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Column {
    /// column name instead of auto convert.
    String value() default "";
    /// indexed with association fields, value should contain self column name.
    String[] indexed() default {};
    /// unique with one or more association fields, value should contain self column name.
    String[] unique() default {};

    /// 1. storage size:
    ///     + same as {@link #max()} for `byte[]` and `Buffer`
    ///     + same as {@link #max()} for `Enum`, `Period`,`String` and `Duration`
    ///
    int size() default -1;

    /// 1. max length
    ///     + required for `byte[]` and `Buffer`
    ///     + optional for `Enum`(default 255), `Period`(default 255) ,`String` (default 255) and `Duration` (default 255)
    ///     + for another types are notation.
    /// 2. max value: only notation.
    int max() default -1;

    /// minimal length or value.This only for notation.
    int min() default -1;

    /// precision for {@link java.math.BigDecimal} and {@link io.vertx.sqlclient.data.Numeric}
    int precision() default -1;

    /// scale for {@link java.math.BigDecimal} and {@link io.vertx.sqlclient.data.Numeric}
    int scale() default -1;

    /// use enumerate name instead of ordinal
    boolean enumName() default false;

    /// static field name hold a {@link activate.api.store.Interceptor}.
    String interceptField() default "";

    /// class contains the interceptor, default is current owner class.
    Class<?> interceptHolder() default void.class;

}
