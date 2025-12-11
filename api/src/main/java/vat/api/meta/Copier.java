package vat.api.meta;

import vat.api.Data;

import java.lang.annotation.*;

/**
 * Mark current data type can copy from other one
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(Copier.List.class)
@Documented
public @interface Copier {
    /// must provide for multiple copy or copy strategy,Base format is `copyFromDataName`.
    String name() default "";

    Class<? extends Data> value();

    /// Process strategy for copy value.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Documented
    @Repeatable(Process.List.class)
    @interface Process {
        /// Allow use default value when source missing value, Otherwise throw system BadRequest error.
        /// when field is missing default values are defined as blow:
        /// + primitive type: zero value
        /// + JsonObject and JsonArray field: empty value
        /// + other object type: null
        boolean withDefault() default false;

        /// Strategy should matches {@link Copier#name()}
        String strategy() default "";

        /// from field, empty for same field name
        String from() default "";

        /// default is current element, hold in orders are `provide,converter,validate`, use `void.class` for not provide.
        Class<?>[] holders() default {};

        ///  The provider field match `Function<SourceData,TargetFieldType>`
        String provide() default "";

        /// The validate field match `Consumer<SourceFieldType>`
        String validate() default "";

        /// The convert field match `BiFunction<SourceData,SourceFieldType,TargetFieldType>`
        String convert() default "";

        @Retention(RetentionPolicy.CLASS)
        @Target(ElementType.METHOD)
        @Documented
        @interface List {
            Process[] value();
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @Documented
    @interface List {
        Copier[] value();
    }
}
