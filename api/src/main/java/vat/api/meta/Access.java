package vat.api.meta;

import java.lang.annotation.*;

///
/// Make an Activities's method as standard storage access method.
/// 1. `Future<Optional<Entity> identityEntityName(long id)`: an Identity method, should only have one or none.
/// 2. `Future<Optional<Ability>> authorizeAbilityName(long userId)`: an Authorize method, should only have one or none.
/// 3. `Future<Entity|Entry> createEntityName(? extends Curd.Creator data)`: a Create method, should only have one or none.
/// 4. `Future<Void> removeEntityName(? extends Curd.Remover data)`: a Remove method, should only have one or none.
/// 5. `Future<Void|Entity.Entry|Entity> updateEntityName(? extends Curd.Modificator data)`: an Update method, can have none or many.
/// 6. *Note*: the prefix of methods is strongly required.
/// 7. *Note*: the product type of Identity and Authorize must be optional.
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface Access {
    /// set when can't provide entity type from method signature.
    Class<?> entity() default void.class;

    /// The copier strategy name: only for update or create.
    String value() default "";

    /// Allow system invoke, which means without actor id provided, and without authorize check.
    boolean allowSystem() default false;


}
