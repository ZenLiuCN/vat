/// Activate declares core type for build domain driven application systems.
/// ## Core concepts
/// 1. Domain: virtual concept for a domain scope.
/// 2. Activities: a group of actions a domain provided. Same as domain service.
/// 3. Data: generic domain data objects.
/// 4. Entity: specialized domain data objects, which are persistence with an identity.
///     + with exposed entity, should have an action with `<T extends Entity> Future<Optional<T>> [name?]Identity(ID id)` to dereference the identity to instance.
/// 5. Actor: specialized entity present an instance of system user, a person or an outer system.
///     + all domains may share with same actor.
/// 6. Ability: specialized entity present an instance of domain role of a system user.
///     + each domain should have one or none.
///     + each domain should have an action of `<T extends Ability > Future<Optional<T>> able(ID user)`, when contains an Ability.
/// 7. Record: specialized entity present an instance of domain record.
/// 8. Event: specialized data present a domain event that may produce or consumed by the defined domain.
/// ## Implementation notes
/// 1. All domain elements should share only one package name.
/// 2. It's easier to defined domain api elements with-in closure of a top level Activities.
/// 3. All domain elements property are not use java bean protocol.
@NullMarked
package vat.api;

import org.jspecify.annotations.NullMarked;