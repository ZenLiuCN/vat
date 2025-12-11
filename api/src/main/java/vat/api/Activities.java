package vat.api;

import io.vertx.core.Vertx;
import vat.api.meta.Errors;

///
/// Domain functions which will registered as event-bus services.
/// 1. Each domain can have one and only one Activities.
/// 2. Activities can have definitions of public default method as contextual requirements:
///     + {@link vat.api.meta.Storage} entity storage that produce a {@link Store}
///     + {@link vat.api.meta.Config} configuration that produce a Json value
///     + {@link Errors} error builder that produce a {@link DomainError}, with or without parameters.
///     + {@link vat.api.meta.Subscribe} event subscriber that subscribe to an {@link Event}
///     + {@link vat.api.meta.Publish} event publisher that publish an {@link Event}
/// 3. Activities must have definitions of public none-default method as actions:
///     + returns {@link io.vertx.core.Future} of serializable data.
///     + accept **one** or none parameter of serializable data.
/// 4. Each action method must have unique name in one Activities.
/// 5. Serializable data:
///     + {@link Data}
///     +  primitive type (and it's boxed type), for input Void is not allowed, for output Void is allowed
/// 6. Activities Domain prototype:
///     + activities will generate a xxxxDomain prototype class for implements.
///     + domain implementation should implement with one of those constructor:
///         + `(vertx,string?)`: simple without configuration required.
///         + `(vertx,string?,jsonObject)`: simple with configuration required.
///         + `(vertx,string?,pool,dialect,jsonObject)`: SQL store with configuration required.
///         + `(vertx,string?,web.factory,jsonObject)`: web endpoint with configuration required.
///         + `(vertx,string?,web.factory,pool,dialect,jsonObject)`: web endpoint and store with configuration required.
///     + domain implementation should always implement with a no-arguments constructor, and registered as service provider of {@link Activities}.
///     + domain implementation should annotate with {@link vat.api.meta.Activity}
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Prototype
public interface Activities extends Domain, Disposable {
    Vertx vertx();


}
