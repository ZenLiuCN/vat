package vat.api.meta;

import java.lang.annotation.*;

///
/// Make an Activities's method as authorized method.
///
/// A doXXX method will be generated to implements. Before that authorization process will execute.
///
/// This can use with {@link Curd}.
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Authorized {
    /// set when can't found ability type from context information, eg: more than one ability or from another domain
    Class<?> ability() default void.class;
    /// The authorize method full name, default to look up the only ability entity as `authorizeXXXX`
    String authorize() default "";
    /// Allow system invoke, which means without actor id provided, and without authorize check.
    boolean allowSystem() default false;
    /// The method holder of functors. default is current domain context.
    Class<?> holder() default Void.class;
    /// The authorize method field, which matches `BiPredicate<? extends Data.Request<?>,@Nullable Ability>`.
    String value() ;
    /// context bad request error provider: when {@link #allowSystem()} is false and request not contains actor.
    String badRequest() default "";
    /// context forbidden error provider: this may have signature `BiFunction<InputType,Ability?,DomainError>` or `Provider<DomainError>`
    String forbidden() default "";

}
