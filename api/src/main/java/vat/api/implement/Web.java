package vat.api.implement;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.buffer.BufferInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SecurityAuditLoggerHandler;
import io.vertx.serviceproxy.ServiceException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vat.api.Activities;
import vat.api.Actor;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.meta.Nullable;
import vat.api.trait.Applicative;
import vat.api.utils.Fn;
import vat.api.utils.Pointer;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface Web extends Router, Applicative<Web> {
    interface RefreshedTokenProvider {
        Optional<String> apply(User user);
    }

    Web withDebug(boolean debug);

    boolean debug();

    interface Factory {
        Web apply(DomainManager manager);
    }

    @Override
    default Web _this() {
        return this;
    }

    Logger log = LoggerFactory.getLogger(Web.class);

    Optional<Authenticator> authenticator();

    DomainManager domains();

    default <T extends Activities> T activities(Class<T> cls, @Nullable String address) {
        return domains().activities(cls, address);
    }

    default <T extends Activities> T activities(Class<T> cls) {
        return domains().activities(cls);
    }

    JsonObject config();

    Router raw();

    static String makePath(String primary, String... segments) {
        if (segments.length == 0) return primary;
        var s = new ArrayList<>(Arrays.asList(segments));
        s.addFirst(primary.charAt(0) == '/' ? primary : ("/" + primary));
        return String.join("/", s);
    }

    default Routing routeOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().route(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing GetOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().get(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PostOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().post(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PutOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().put(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing DeleteOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().delete(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PatchOf(Function<JsonObject, String> path, String... segment) {
        return new E(debug(), raw().patch(makePath(path.apply(config()), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing RouteOf(String pointer, String... segment) {
        return new E(debug(), raw().route(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing GetOf(String pointer, String... segment) {
        return new E(debug(), raw().get(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PostOf(String pointer, String... segment) {
        return new E(debug(), raw().post(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PutOf(String pointer, String... segment) {
        return new E(debug(), raw().put(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing DeleteOf(String pointer, String... segment) {
        return new E(debug(), raw().delete(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing PatchOf(String pointer, String... segment) {
        return new E(debug(), raw().patch(makePath(Pointer.of(pointer).getString(config()).orElseThrow(), segment)), config(), authenticator().orElse(null), domains());
    }

    default Routing Route(String path) {
        return new E(debug(), raw().route(makePath(path)), config(), authenticator().orElse(null), domains());
    }

    default Routing Get(String path) {
        return new E(debug(), raw().get(makePath(path)), config(), authenticator().orElse(null), domains());
    }

    default Routing Post(String path) {
        return new E(debug(), raw().post(makePath(path)), config(), authenticator().orElse(null), domains());
    }

    default Routing Put(String path) {
        return new E(debug(), raw().put(makePath(path)), config(), authenticator().orElse(null), domains());
    }

    default Routing Delete(String path) {
        return new E(debug(), raw().delete(makePath(path)), config(), authenticator().orElse(null), domains());
    }

    default Routing Patch(String path) {
        return new E(debug(), raw().patch(makePath(path)), config(), authenticator().orElse(null), domains());
    }


    //region Delegate

    @Override
    default Map<String, Object> metadata() {
        return raw().metadata();
    }

    @Override
    default <T> T getMetadata(String key) {
        return raw().getMetadata(key);
    }


    @Override
    default Route route() {
        return raw().route();
    }

    @Override
    default Route route(HttpMethod httpMethod, String s) {
        return raw().route(httpMethod, s);
    }

    @Override
    default Route route(String s) {
        return raw().route(s);
    }

    @Override
    default Route routeWithRegex(HttpMethod httpMethod, String s) {
        return raw().routeWithRegex(httpMethod, s);
    }

    @Override
    default Route routeWithRegex(String s) {
        return raw().routeWithRegex(s);
    }

    @Override
    default Route get() {
        return raw().get();
    }

    @Override
    default Route get(String s) {
        return raw().get(s);
    }

    @Override
    default Route getWithRegex(String s) {
        return raw().getWithRegex(s);
    }

    @Override
    default Route head() {
        return raw().head();
    }

    @Override
    default Route head(String s) {
        return raw().head(s);
    }

    @Override
    default Route headWithRegex(String s) {
        return raw().headWithRegex(s);
    }

    @Override
    default Route options() {
        return raw().options();
    }

    @Override
    default Route options(String s) {
        return raw().options(s);
    }

    @Override
    default Route optionsWithRegex(String s) {
        return raw().optionsWithRegex(s);
    }

    @Override
    default Route put() {
        return raw().put();
    }

    @Override
    default Route put(String s) {
        return raw().put(s);
    }

    @Override
    default Route putWithRegex(String s) {
        return raw().putWithRegex(s);
    }

    @Override
    default Route post() {
        return raw().post();
    }

    @Override
    default Route post(String s) {
        return raw().post(s);
    }

    @Override
    default Route postWithRegex(String s) {
        return raw().postWithRegex(s);
    }

    @Override
    default Route delete() {
        return raw().delete();
    }

    @Override
    default Route delete(String s) {
        return raw().delete(s);
    }

    @Override
    default Route deleteWithRegex(String s) {
        return raw().deleteWithRegex(s);
    }

    @Override
    default Route trace() {
        return raw().trace();
    }

    @Override
    default Route trace(String s) {
        return raw().trace(s);
    }

    @Override
    default Route traceWithRegex(String s) {
        return raw().traceWithRegex(s);
    }

    @Override
    default Route connect() {
        return raw().connect();
    }

    @Override
    default Route connect(String s) {
        return raw().connect(s);
    }

    @Override
    default Route connectWithRegex(String s) {
        return raw().connectWithRegex(s);
    }

    @Override
    default Route patch() {
        return raw().patch();
    }

    @Override
    default Route patch(String s) {
        return raw().patch(s);
    }

    @Override
    default Route patchWithRegex(String s) {
        return raw().patchWithRegex(s);
    }

    @Override
    default List<Route> getRoutes() {
        return raw().getRoutes();
    }


    @Override
    default void handleContext(RoutingContext routingContext) {
        raw().handleContext(routingContext);
    }

    @Override
    default void handleFailure(RoutingContext routingContext) {
        raw().handleFailure(routingContext);
    }


    @Override
    default void handle(HttpServerRequest event) {
        raw().handle(event);
    }

    //endregion

    //region Overrides
    @Override
    default Web putMetadata(String s, Object o) {
        raw().putMetadata(s, o);
        return this;
    }


    @Override
    default Web clear() {
        raw().clear();
        return this;
    }

    @Override
    default Web errorHandler(int i, Handler<RoutingContext> handler) {
        raw().errorHandler(i, handler);
        return this;
    }

    @Override
    default Web modifiedHandler(Handler<Router> handler) {
        raw().modifiedHandler(handler);
        return this;
    }

    @Override
    default Web allowForward(AllowForwardHeaders allowForwardHeaders) {
        raw().allowForward(allowForwardHeaders);
        return this;
    }

    //endregion

    static R of(Router router, JsonObject config, @Nullable Authenticator authenticator, DomainManager manager) {
        return new R(false, router, config, authenticator, manager);
    }

    static Factory of(Router router, JsonObject config, @Nullable Authenticator authenticator) {
        return d -> of(router, config, authenticator, d);
    }

    record R(@With boolean debug, Router raw, JsonObject config, @Nullable Authenticator authenticate,
             DomainManager domains) implements Web {

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.ofNullable(authenticate);
        }
    }

    interface Context extends RoutingContext {
        boolean debug();

        Optional<Authenticator> authenticator();

        DomainManager domains();

        default <T extends Activities> T activities(Class<T> cls, @Nullable String address) {
            return domains().activities(cls, address);
        }

        default <T extends Activities> T activities(Class<T> cls) {
            return domains().activities(cls);
        }

        JsonObject config();


        RoutingContext raw();

        static String remoteRealAddress(RoutingContext raw) {
            var header = raw.request().headers();
            return Optional.ofNullable(header.get("x-forwarded-for"))
                    .filter(ip -> !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim()))
                    .flatMap(ip -> Arrays.stream(ip.split(",")).findFirst())
                    .or(() -> Optional.ofNullable(header.get("Proxy-Client-IP"))
                            .filter(ip -> !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim()))
                    )
                    .or(() -> Optional.ofNullable(header.get("WL-Proxy-Client-IP"))
                            .filter(ip -> !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim()))
                    )
                    .or(() -> Optional.ofNullable(header.get("Remote Address"))
                            .filter(ip -> !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim()))
                            .flatMap(ip -> Arrays.stream(ip.split(":")).findFirst())
                    )
                    .orElse("");
        }

        default String remoteAddress() {
            return remoteRealAddress(raw());
        }

        //region Variables
        default List<String> varQueries(String name) {
            return queryParam(name);
        }

        default Optional<String> varQuery(String name) {
            return Optional.ofNullable(queryParam(name)).map(x -> x.isEmpty() ? null : x.getFirst());
        }

        default Optional<String> varPath(String name) {
            return Optional.ofNullable(pathParam(name));
        }

        default Optional<String> varHeader(String name) {
            return Optional.ofNullable(request().getHeader(name));
        }

        default <R> Optional<R> varQuery(String name, Function<String, R> map) {
            return varQuery(name)
                    .filter(Predicate.not(String::isBlank))
                    .map(map);
        }

        default <R> Optional<R> varHeader(String name, Function<String, R> map) {
            return varHeader(name).filter(Predicate.not(String::isBlank)).map(map);
        }

        default <R> Optional<R> varPath(String name, Function<String, R> map) {
            return varPath(name).filter(Predicate.not(String::isBlank)).map(map);
        }

        default <R> R varQuery$(String name, Function<String, R> map) {
            return varQuery(name, map)
                    .orElseThrow(() -> DomainError.System.badRequest("", ""));
        }

        default <R> R varHeader$(String name, Function<String, R> map) {
            return varHeader(name, map)
                    .orElseThrow(() -> DomainError.System.badRequest("", ""));
        }

        default <R> R varPath$(String name, Function<String, R> map) {
            return varPath(name, map)
                    .orElseThrow(() -> DomainError.System.badRequest("", ""));
        }

        //endregion

        //region Response send

        static <T> Handler<AsyncResult<T>> handler(
                RoutingContext ctx,
                boolean debug,
                Runnable before, BiConsumer<RoutingContext, T> handle) {
            return debug ? r -> {
                before.run();
                if (r.succeeded()) {
                    handle.accept(ctx, r.result());
                } else {
                    log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } : r -> {
                before.run();
                if (r.succeeded()) {
                    handle.accept(ctx, r.result());
                } else {
                    ctx.fail(r.cause());
                }
            };
        }

        /// send none converted json data
        default <T extends Data> Handler<AsyncResult<T>> respondOne() {
            return respondOne(null);
        }

        /// send none converted json data
        default <T extends Data> Handler<AsyncResult<T>> respondOne(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> c.json(toJson(v)));
        }

        /// send none converted json data list
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondList() {
            return respondList(null);
        }

        /// send none converted json data list
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondList(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> c.json(toJson(v)));
        }

        /// send  converted json data for js compatible
        default <T extends Data> Handler<AsyncResult<T>> respondOneJs() {
            return respondOneJs(null);
        }

        /// send  converted json data for js compatible
        default <T extends Data> Handler<AsyncResult<T>> respondOneJs(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> c.json(toJS(v)));
        }

        /// send  converted json data list for js compatible
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListJs() {
            return respondListJs(null);
        }

        /// send  converted json data list for js compatible
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListJs(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> c.json(toJS(v)));
        }

        /// send raw json data
        default Handler<AsyncResult<JsonObject>> respond() {
            return respond(null);
        }

        /// send raw json data
        default Handler<AsyncResult<JsonObject>> respond(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, RoutingContext::json);
        }

        /// send raw json data
        default Handler<AsyncResult<JsonArray>> respondArray() {
            return respondArray(null);
        }

        /// send raw json data
        default Handler<AsyncResult<JsonArray>> respondArray(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, RoutingContext::json);
        }

        /// Manually response process
        default <T> Handler<AsyncResult<T>> response(BiFunction<Context, T, Future<Void>> response) {
            return response(response, null);
        }

        /// Manually response process
        default <T> Handler<AsyncResult<T>> response(BiFunction<Context, T, Future<Void>> response, @Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (s, v) -> {
                var f = response.apply(this, v);
                if (f != null) {
                    f.onFailure(ctx::fail);
                }
            });
        }

        /// send nothing
        default Handler<AsyncResult<Void>> respondVoid() {
            return respondVoid(null);
        }

        /// send nothing
        default Handler<AsyncResult<Void>> respondVoid(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, n) -> c.end());
        }

        /// send raw text
        default Handler<AsyncResult<String>> respondText() {
            return respondText(null);
        }

        /// send raw text
        default Handler<AsyncResult<String>> respondText(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> Web.contentType(ctx, HttpHeaderValues.TEXT_PLAIN).end(v));
        }

        /// send raw html string
        default Handler<AsyncResult<String>> respondHtmlText() {
            return respondHtmlText(null);
        }

        /// send raw html string
        default Handler<AsyncResult<String>> respondHtmlText(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> Web.contentType(ctx, HttpHeaderValues.TEXT_HTML).end(v));
        }

        /// send raw html buffer
        default Handler<AsyncResult<Buffer>> respondHtmlBuffer() {
            return respondHtmlBuffer(null);
        }

        /// send raw html buffer
        default Handler<AsyncResult<Buffer>> respondHtmlBuffer(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> Web.contentType(ctx, HttpHeaderValues.TEXT_HTML).end(v));
        }

        /// send with data container
        default <T extends Data> Handler<AsyncResult<T>> respondOneJsData() {
            return respondOneJsData(null);
        }

        /// send with data container
        default <T extends Data> Handler<AsyncResult<T>> respondOneJsData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", toJS(v))));
        }

        /// send with data container
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListJsData() {
            return respondListJsData(null);
        }

        /// send with data container
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListJsData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", toJS(v))));
        }

        /// send with data container
        default <T extends Data> Handler<AsyncResult<T>> respondOneData() {
            return respondOneData(null);

        }

        /// send with data container
        default <T extends Data> Handler<AsyncResult<T>> respondOneData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", toJson(v))));
        }

        /// send with data container
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListData() {
            return respondListData(null);
        }

        /// send with data container
        default <T extends Data, L extends Collection<T>> Handler<AsyncResult<L>> respondListData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", toJson(v))));
        }


        default Handler<AsyncResult<JsonObject>> respondData() {
            return respondData(null);
        }

        default Handler<AsyncResult<JsonObject>> respondData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", v)));
        }

        default Handler<AsyncResult<JsonArray>> respondArrayData() {
            return respondArrayData(null);
        }

        default Handler<AsyncResult<JsonArray>> respondArrayData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", v)));
        }

        default Handler<AsyncResult<Void>> respondVoidData() {
            return respondVoidData(null);
        }

        default Handler<AsyncResult<Void>> respondVoidData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", v)));
        }

        default Handler<AsyncResult<String>> respondTextData() {
            return respondTextData(null);
        }

        default Handler<AsyncResult<String>> respondTextData(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> ctx.json(JsonObject.of("code", 200, "data", v)));
        }

        default Handler<AsyncResult<String>> redirectTo() {
            return redirectTo(null);
        }

        default Handler<AsyncResult<String>> redirectTo(@Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before, (c, v) -> c.redirect(v)
                    .onComplete(a -> {
                        if (a.failed()) {
                            log.error("redirect to failure", a.cause());
                            ctx.fail(a.cause());
                        }
                    }));
        }

        default Handler<AsyncResult<Buffer>> binary(String contentType, @Nullable String fileName) {
            return binary(contentType, fileName, null);
        }

        default Handler<AsyncResult<Buffer>> binary(String contentType, @Nullable String fileName, @Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            var type = Fn.notBlank(contentType) ? contentType : HttpHeaderValues.APPLICATION_OCTET_STREAM;
            return handler(ctx, debug, before,
                    Fn.notBlank(fileName) ? (c, v) -> Web.contentType(c, type)
                            .putHeader(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment;filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                            .setStatusCode(200).send(v)
                            : (c, v) -> Web.contentType(c, type).setStatusCode(200).send(v)
            );
        }

        default <T> Handler<AsyncResult<T>> binary(Function<T, Buffer> binary,
                                                   Function<T, String> contentType,
                                                   Function<T, String> fileName) {
            return binary(binary, contentType, fileName, null);
        }

        default <T> Handler<AsyncResult<T>> binary(Function<T, Buffer> binary,
                                                   Function<T, String> contentType,
                                                   Function<T, String> fileName,
                                                   @Nullable RefreshedTokenProvider provider) {
            var ctx = raw();
            var debug = debug();
            var before = injectRefreshedToken(provider, ctx);
            return handler(ctx, debug, before,
                    (c, v) -> Web.contentType(c, contentType.apply(v))
                            .putHeader(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment;filename*=UTF-8''" + URLEncoder.encode(fileName.apply(v), StandardCharsets.UTF_8))
                            .setStatusCode(200).send(binary.apply(v))

            );
        }
        //endregion


        //region Error
        default void info(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(Web.error(status, message, DomainError.MODE_NOTIFY));
        }

        default void error(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(Web.error(status, message, DomainError.MODE_PROMPT));
        }

        default void fatal(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(Web.error(status, message, DomainError.MODE_FATAL));
        }

        default void withInfo(JsonArray data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_NOTIFY));
        }

        default void withInfo(Number data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_NOTIFY));
        }

        default void withInfo(JsonObject data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_NOTIFY));
        }


        default void withError(JsonArray data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_PROMPT));
        }

        default void withError(Number data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_PROMPT));
        }

        default void withError(JsonObject data, @Nullable String message) {
            raw().json(Web.ok(data, message, DomainError.MODE_PROMPT));
        }
        //endregion

        //region delegate
        @Override
        default HttpServerRequest request() {
            return raw().request();
        }

        @Override
        default HttpServerResponse response() {
            return raw().response();
        }

        @Override
        default void next() {
            raw().next();
        }

        @Override
        default void fail(int statusCode) {
            raw().fail(statusCode);
        }

        @Override
        default void fail(Throwable throwable) {
            raw().fail(throwable);
        }

        @Override
        default void fail(int statusCode, Throwable throwable) {
            raw().fail(statusCode, throwable);
        }


        @Override
        default <T> T get(String key) {
            return raw().get(key);
        }

        @Override
        default <T> T get(String key, T defaultValue) {
            return raw().get(key, defaultValue);
        }

        @Override
        default <T> T remove(String key) {
            return raw().remove(key);
        }

        @Override
        default <T> Map<String, T> data() {
            return raw().data();
        }

        @Override
        default Vertx vertx() {
            return raw().vertx();
        }

        @Override
        default String mountPoint() {
            return raw().mountPoint();
        }

        @Override

        default Route currentRoute() {
            return raw().currentRoute();
        }

        @Override
        default String normalizedPath() {
            return raw().normalizedPath();
        }


        @Override
        default RequestBody body() {
            return raw().body();
        }

        @Override
        default List<FileUpload> fileUploads() {
            return raw().fileUploads();
        }

        @Override
        default void cancelAndCleanupFileUploads() {
            raw().cancelAndCleanupFileUploads();
        }

        @Deprecated
        @Override
        default Session session() {
            return raw().session();
        }

        @Override
        default boolean isSessionAccessed() {
            return raw().isSessionAccessed();
        }

        @Override
        default User user() {
            return raw().user();
        }

        @Override
        default Throwable failure() {
            return raw().failure();
        }

        @Override
        default int statusCode() {
            return raw().statusCode();
        }

        @Override
        default String getAcceptableContentType() {
            return raw().getAcceptableContentType();
        }

        @Override
        default ParsedHeaderValues parsedHeaders() {
            return raw().parsedHeaders();
        }

        @Deprecated
        @Override
        default int addHeadersEndHandler(Handler<Void> handler) {
            return raw().addHeadersEndHandler(handler);
        }

        @Deprecated
        @Override
        default boolean removeHeadersEndHandler(int handlerID) {
            return raw().removeHeadersEndHandler(handlerID);
        }

        @Deprecated
        @Override
        default int addBodyEndHandler(Handler<Void> handler) {
            return raw().addBodyEndHandler(handler);
        }

        @Deprecated
        @Override
        default boolean removeBodyEndHandler(int handlerID) {
            return raw().removeBodyEndHandler(handlerID);
        }

        @Deprecated
        @Override
        default int addEndHandler(Handler<AsyncResult<Void>> handler) {
            return raw().addEndHandler(handler);
        }


        @Deprecated
        @Override
        default boolean removeEndHandler(int handlerID) {
            return raw().removeEndHandler(handlerID);
        }

        @Override
        default boolean failed() {
            return raw().failed();
        }


        @Override
        default void setAcceptableContentType(String contentType) {
            raw().setAcceptableContentType(contentType);
        }

        @Override
        default void reroute(String path) {
            raw().reroute(path);
        }

        @Override
        default void reroute(HttpMethod method, String path) {
            raw().reroute(method, path);
        }

        @Override
        default List<LanguageHeader> acceptableLanguages() {
            return raw().acceptableLanguages();
        }

        @Override
        default LanguageHeader preferredLanguage() {
            return raw().preferredLanguage();
        }

        @Override
        default Map<String, String> pathParams() {
            return raw().pathParams();
        }

        @Override
        default String pathParam(String name) {
            return raw().pathParam(name);
        }

        @Override
        default MultiMap queryParams() {
            return raw().queryParams();
        }

        @Override
        default MultiMap queryParams(Charset encoding) {
            return raw().queryParams(encoding);
        }

        @Override
        default List<String> queryParam(String name) {
            return raw().queryParam(name);
        }


        @Override
        default Future<Void> redirect(String url) {
            return raw().redirect(url);
        }


        @Override
        default Future<Void> json(Object json) {
            return raw().json(json);
        }


        @Override
        default boolean is(String type) {
            return raw().is(type);
        }

        @Override
        default boolean isFresh() {
            return raw().isFresh();
        }

        @Override
        default Future<Void> end(String chunk) {
            return raw().end(chunk);
        }


        @Override
        default Future<Void> end(Buffer buffer) {
            return raw().end(buffer);
        }


        @Override
        default Future<Void> end() {
            return raw().end();
        }


        //endregion

        //region overrides


        @Override
        default UserContext userContext() {
            return raw().userContext();
        }

        @Override
        default Context attachment(String filename) {
            raw().attachment(filename);
            return this;
        }


        @Override
        default Context etag(String etag) {
            raw().etag(etag);
            return this;
        }

        @Override
        default Context put(String key, Object obj) {
            raw().put(key, obj);
            return this;
        }

        @Override
        default Context lastModified(Instant instant) {
            raw().lastModified(instant);
            return this;
        }

        @Override
        default Context lastModified(String instant) {
            raw().lastModified(instant);
            return this;
        }


        //endregion

        default Context contentType(String contentType) {
            raw().response().headers().add(HttpHeaderNames.CONTENT_TYPE,
                    contentType == null || contentType.isBlank()
                            ? HttpHeaderValues.APPLICATION_OCTET_STREAM
                            : contentType);
            return this;
        }

        default Context contentDisposition(String fileName) {
            raw().response().headers().add(HttpHeaderNames.CONTENT_DISPOSITION,
                    fileName == null || fileName.isBlank()
                            ? "attachment; filename=\"file\""
                            : "attachment; filename*=UTF-8''%s".formatted(URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                            .replace("+", "%20")));
            return this;
        }

        default <T> Handler<AsyncResult<Void>> sse(Function<EventEmitter, T> onConnect, Handler<T> onFinish) {
            return rx -> {
                if (rx.succeeded()) {
                    var res = raw().response();
                    res
                            .putHeader("Content-Type", "text/event-stream")
                            .putHeader("Connection", "keep-alive")
                            .putHeader("Transfer-Encoding", "chunked")
                            .putHeader("Cache-Control", "no-cache")
                            .putHeader("X-Accel-Buffering", "no")
                            .putHeader("Cache-Control", "no-cache, must-revalidate")
                            .setStatusCode(200)
                            .setChunked(true)
                            .write(EMPTY);
                    var r = onConnect.apply(() -> res);
                    res.closeHandler($ -> {
                        onFinish.handle(r);
                        if (!res.ended()) res.end();
                    }).endHandler($ -> {
                        onFinish.handle(r);
                        if (!res.ended()) res.end();
                    });
                } else {
                    if (debug()) log.error("fail {}", Web.dump(this.raw()), rx.cause());
                    this.raw().fail(rx.cause());
                }
            };
        }

        default <T> Handler<AsyncResult<Void>> sse(BiFunction<Context, EventEmitter, T> onConnect, Handler<T> onFinish) {
            return rx -> {
                if (rx.succeeded()) {
                    var res = raw().response();
                    res
                            .putHeader("Content-Type", "text/event-stream")
                            .putHeader("Connection", "keep-alive")
                            .putHeader("Transfer-Encoding", "chunked")
                            .putHeader("Cache-Control", "no-cache")
                            .putHeader("X-Accel-Buffering", "no")
                            .putHeader("Cache-Control", "no-cache, must-revalidate")
                            .setStatusCode(200)
                            .setChunked(true)
                            .write(EMPTY);
                    var r = onConnect.apply(this, () -> res);
                    res.closeHandler($ -> {
                        onFinish.handle(r);
                        if (!res.ended()) res.end();
                    }).endHandler($ -> {
                        onFinish.handle(r);
                        if (!res.ended()) res.end();
                    });
                } else {
                    if (debug()) log.error("fail {}", Web.dump(this.raw()), rx.cause());
                    this.raw().fail(rx.cause());
                }
            };
        }


        @EqualsAndHashCode
        class C implements Context {
            @Getter
            @Accessors(fluent = true)

            final boolean debug;
            @Getter
            @Accessors(fluent = true)
            final Authenticator authenticate;
            @Getter
            @Accessors(fluent = true)
            final RoutingContext raw;
            @Getter
            @Accessors(fluent = true)
            final JsonObject config;

            @Getter
            @Accessors(fluent = true)
            final DomainManager domains;

            public C(boolean debug, RoutingContext raw, Authenticator authenticate, JsonObject config, DomainManager domains) {
                this.debug = debug;
                this.raw = raw;
                this.config = config;
                this.domains = domains;
                this.authenticate = authenticate;
            }

            @Override
            public Optional<Authenticator> authenticator() {
                return Optional.ofNullable(authenticate);
            }
        }


    }

    interface AuthorityContext extends Context {

        @EqualsAndHashCode(callSuper = true)
        class C extends Context.C implements AuthorityContext {

            C(boolean debug, RoutingContext raw, Authenticator authenticate, JsonObject config, DomainManager domains) {
                super(debug, raw, authenticate, config, domains);
            }

        }


        default <T extends Actor> Optional<T> user(Function<JsonObject, T> map) {
            return Optional.ofNullable(raw().user()).map(User::principal).map(map);
        }

        default <T, U extends Data> Future<Optional<U>> user(Function<JsonObject, T> map,
                                                             Function<T, Future<U>> loader) {
            return Optional.ofNullable(raw().user()).map(User::principal).map(map)
                    .map(loader)
                    .map(f -> f.map(Optional::ofNullable))
                    .orElseGet(() -> Future.succeededFuture(Optional.empty()));
        }


    }

    interface BodyContext<T extends Data> extends Context {
        @EqualsAndHashCode(callSuper = true)
        class C<T extends Data> extends Context.C implements BodyContext<T> {
            @Getter
            @Accessors(fluent = true)
            final T payload;

            C(boolean debug, RoutingContext raw, Authenticator authenticate, JsonObject config, T payload, DomainManager domains) {
                super(debug, raw, authenticate, config, domains);
                this.payload = payload;
                if (payload instanceof Data.Validation<?> v) {
                    v.doValidate();
                }
            }

        }

        T payload();
    }

    interface BodyAuthorityContext<T extends Data> extends Context {
        @EqualsAndHashCode(callSuper = true)
        class C<T extends Data> extends Context.C implements BodyAuthorityContext<T> {
            @Getter
            @Accessors(fluent = true)
            final T payload;

            C(boolean debug, RoutingContext raw, Authenticator authenticate, JsonObject config, T payload, DomainManager domains) {
                super(debug, raw, authenticate, config, domains);
                this.payload = payload;
                if (payload instanceof Data.Validation<?> v) {
                    v.doValidate();
                }
            }


        }

        T payload();
    }

    interface Routing extends Route, Applicative<Routing> {

        boolean debug();

        @Override
        default Routing _this() {
            return this;
        }

        Optional<Authenticator> authenticator();

        DomainManager domains();

        default <T extends Activities> T activities(Class<T> cls, @Nullable String address) {
            return domains().activities(cls, address);
        }

        default <T extends Activities> T activities(Class<T> cls) {
            return domains().activities(cls);
        }

        JsonObject config();

        Route raw();

        default Routing securityLogging() {
            raw().handler(SecurityAuditLoggerHandler.create());
            return this;
        }

        //region Delegate


        @Override
        default Map<String, Object> metadata() {
            return raw().metadata();
        }

        @Override
        default <T> T getMetadata(String key) {
            return raw().getMetadata(key);
        }

        @Override
        default String getPath() {
            return raw().getPath();
        }

        @Override
        default boolean isRegexPath() {
            return raw().isRegexPath();
        }

        @Override
        default boolean isExactPath() {
            return raw().isExactPath();
        }

        @Override
        default Set<HttpMethod> methods() {
            return raw().methods();
        }


        @Override
        default String getName() {
            return raw().getName();
        }


        //endregion

        //region Override

        @Override
        default Router getSubRouter() {
            return raw().getSubRouter();
        }

        @Override
        default Routing setRegexGroupsNames(List<String> list) {
            raw().setRegexGroupsNames(list);
            return this;
        }

        @Override
        default Routing setName(String s) {
            raw().setName(s);
            return this;
        }

        @Override
        default <T> Routing respond(Function<RoutingContext, Future<T>> function) {
            raw().respond(function);
            return this;
        }

        @Override
        default Routing putMetadata(String s, Object o) {
            raw().putMetadata(s, o);
            return this;
        }

        @Override
        default Routing method(HttpMethod httpMethod) {
            raw().method(httpMethod);
            return this;
        }

        @Override
        default Routing path(String s) {
            raw().path(s);
            return this;
        }

        @Override
        default Routing pathRegex(String s) {
            raw().pathRegex(s);
            return this;
        }

        @Override
        default Routing produces(String s) {
            raw().produces(s);
            return this;
        }

        @Override
        default Routing consumes(String s) {
            raw().consumes(s);
            return this;
        }

        @Override
        default Routing virtualHost(String s) {
            raw().virtualHost(s);
            return this;
        }

        @Override
        default Routing order(int i) {
            raw().order(i);
            return this;
        }

        @Override
        default Routing last() {
            raw().last();
            return this;
        }

        @Override
        default Route handler(Handler<RoutingContext> handler) {
            return raw().handler(handler);
        }

        @Override
        default Route blockingHandler(Handler<RoutingContext> handler) {
            return raw().blockingHandler(handler);
        }

        @Override
        default Routing subRouter(Router router) {
            raw().subRouter(router);
            return this;
        }

        default Routing subRouter(Web router) {
            raw().subRouter(router.raw());
            return this;
        }

        @Override
        default Route blockingHandler(Handler<RoutingContext> handler, boolean b) {
            return raw().blockingHandler(handler, b);
        }

        @Override
        default Routing failureHandler(Handler<RoutingContext> handler) {
            raw().failureHandler(handler);
            return this;
        }

        @Override
        default Routing remove() {
            raw().remove();
            return this;
        }

        @Override
        default Routing disable() {
            raw().disable();
            return this;
        }

        @Override
        default Routing enable() {
            raw().enable();
            return this;
        }

        @Override
        default Routing useNormalizedPath(boolean b) {
            raw().useNormalizedPath(b);
            return this;
        }

        //endregion

        //region Methods
        default <T extends Data> Route Body(
                Function<JsonObject, T> read,
                Consumer<BodyContext<T>> action) {
            return raw().consumes(HttpHeaderValues.APPLICATION_JSON.toString())
                    .handler(BodyHandler.create())
                    .handler(c -> action.accept(new BodyContext.C<>(
                            debug(),
                            c,
                            authenticator().orElse(null),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default <T extends Data> Route Body(
                Function<JsonObject, T> read,
                UnaryOperator<BodyHandler> conf,
                Consumer<BodyContext<T>> action) {
            return raw().consumes(HttpHeaderValues.APPLICATION_JSON.toString())
                    .handler((conf == null ? UnaryOperator.<BodyHandler>identity() : conf).apply(BodyHandler.create()))
                    .handler(c -> action.accept(new BodyContext.C<>(
                            debug(),
                            c,
                            authenticator().orElse(null),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default Route Auth(Consumer<Context> action) {
            return raw()
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticate())
                    .handler(c -> action.accept(new AuthorityContext.C(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            domains()
                    )));
        }

        default <T extends Data> Route AuthBody(
                Function<JsonObject, T> read,
                UnaryOperator<BodyHandler> conf,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler((conf == null ? UnaryOperator.<BodyHandler>identity() : conf).apply(BodyHandler.create()))
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticate())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default <T extends Data> Route AuthBody(
                Function<JsonObject, T> read,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler(BodyHandler.create())
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticate())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default <T extends Data> Route AuthAnyBody(
                Function<RequestBody, T> read,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler(BodyHandler.create())
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticate())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body()),
                            domains()
                    )));
        }

        default Route OptionalAuth(
                Consumer<Context> action) {
            return raw()
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticateMaybe())
                    .handler(c -> action.accept(new AuthorityContext.C(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            domains()
                    )));
        }

        default <T extends Data> Route OptionalAuthBody(
                Function<JsonObject, T> read,
                UnaryOperator<BodyHandler> conf,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler((conf == null ? UnaryOperator.<BodyHandler>identity() : conf).apply(BodyHandler.create()))
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticateMaybe())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default <T extends Data> Route OptionalAuthBody(
                Function<JsonObject, T> read,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler(BodyHandler.create())
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticateMaybe())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body().asJsonObject()),
                            domains()
                    )));
        }

        default <T extends Data> Route OptionalAuthAnyBody(
                Function<RequestBody, T> read,
                Consumer<BodyAuthorityContext<T>> action) {
            return raw()
                    .consumes(HttpHeaderValues.APPLICATION_JSON.toString()).handler(BodyHandler.create())
                    .handler(authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")).authenticateMaybe())
                    .handler(c -> action.accept(new BodyAuthorityContext.C<>(debug(),
                            c,
                            authenticator().orElseThrow(() -> DomainError.System.internalServerError("missing authenticator")),
                            config(),
                            read.apply(c.body()),
                            domains()
                    )));
        }

        default Route TextBody(Consumer<Context> action) {
            return Body(HttpHeaderValues.TEXT_PLAIN.toString(), action);
        }

        default Route JsonBody(Consumer<Context> action) {
            return Body(HttpHeaderValues.APPLICATION_JSON.toString(), action);
        }

        default Route Body(String contentType, Consumer<Context> action) {
            return raw()
                    .consumes(contentType)
                    .handler(BodyHandler.create())
                    .handler(c -> action.accept(new Context.C(debug(),
                            c,
                            authenticator().orElse(null),
                            config(),
                            domains()
                    )));
        }

        default Route AnyBody(Consumer<Context> action) {
            return raw()
                    .handler(BodyHandler.create())
                    .handler(c -> action.accept(new Context.C(debug(),
                            c,
                            authenticator().orElse(null),
                            config(),
                            domains()
                    )));
        }

        default Route Accept(Consumer<Context> action) {
            return raw()
                    .handler(c -> action.accept(new Context.C(debug(),
                            c,
                            authenticator().orElse(null),
                            config(),
                            domains()
                    )));
        }

        //endregion
    }

    record E(boolean debug, Route raw, JsonObject config, Authenticator authenticate,
             DomainManager domains) implements Routing {
        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.ofNullable(authenticate);
        }
    }

    static CharSequence dump(RoutingContext ctx) {
        var s = new StringBuilder();
        s.append("\nFROM:").append(ctx.request().remoteAddress())
                .append("\nMETHOD:").append(ctx.request().method())
                .append("\nPATH:").append(ctx.request().path())
                .append("\nQUERY:").append(ctx.request().query())
                .append("\nHEADERS:\n").append(ctx.request().headers());
        return s;
    }


    /// @param domain   the domain identifier of ability
    /// @param identity the identity of ability
    /// @param value    the permission value of ability
    record Authority(
            String domain,
            long identity,
            JsonObject value
    ) implements Data {

        public Authority(JsonObject v) {
            this(v.getString("domain"), v.getLong("identity"), v.getJsonObject("value"));
        }

        @Override
        public JsonObject toJson() {
            return JsonObject.of(
                    "domain", domain,
                    "identity", identity,
                    "value", value
            );
        }

        @Override
        public JsonObject asJson() {
            return toJson();
        }

        @Override
        public JsonObject toJS() {
            return toJson();
        }
    }

    interface Author extends Data, Authorization {
        @Nullable
        String domain();

        boolean check(MultiMap variable, User user);

        @Override
        default boolean match(AuthorizationContext context) {
            Objects.requireNonNull(context);
            return check(context.variables(), context.user());
        }

        @Override
        default boolean verify(Authorization authorization) {
            if (authorization instanceof Author a) {
                return Objects.equals(a.domain(), domain());
            }
            return false;
        }

        @Override
        JsonObject toJson();
    }

    interface AuthorityProvider {
        Future<List<Authority>> load(long identity);
    }

    AtomicReference<String> REFRESH_TOKEN_KEY = new AtomicReference<>("X_NEW_TOKEN");
    Runnable NOOP = () -> {
    };

    static Runnable injectRefreshedToken(Web.RefreshedTokenProvider provider, RoutingContext ctx) {
        if (provider == null) return NOOP;
        return () ->
                Optional.ofNullable(ctx.user())
                        .flatMap(provider::apply)
                        .ifPresent(string -> ctx.response().putHeader(REFRESH_TOKEN_KEY.get(), string));
    }

    static JsonObject toJson(Data d) {
        return d == null ? null : d.toJson();
    }

    static JsonObject toJS(Data d) {
        return d == null ? null : d.toJS();
    }

    static JsonArray toJS(Collection<? extends Data> v) {
        return v == null ? null : new JsonArray(v.stream().map(Fn.nullable(Data::toJS)).toList());
    }

    static JsonArray toJson(Collection<? extends Data> v) {
        return v == null ? null : new JsonArray(v.stream().map(Fn.nullable(Data::toJson)).toList());
    }

    static HttpServerResponse contentType(RoutingContext ctx, CharSequence contentType) {
        var res = ctx.response();
        if (res.headers().contains(HttpHeaders.CONTENT_TYPE) || contentType == null) return res;
        res.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        return res;
    }

    AtomicReference<BiPredicate<Throwable, RoutingContext>> THROWABLE_CONVERT = new AtomicReference<>((err, ctx) -> false);

    Handler<RoutingContext> DEFAULT_500_HANDLER = c -> {
        var cause = c.failure();
        if (log.isDebugEnabled()) {
            log.error("response {} error ", Web.dump(c), cause);
        } else {
            log.error("response error ", cause);
        }
        if (THROWABLE_CONVERT.get().test(cause, c)) {
            return;
        }
        if (cause instanceof DomainError de) {
            c
                    .response()
                    .setStatusCode(de.code)
                    .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .send(error(de.code, de.user, de.mode).toBuffer());
            return;
        }
        if (cause instanceof ServiceException de && de.failureCode() != 500) {
            c
                    .response()
                    .setStatusCode(de.failureCode())
                    .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .send(error(de.failureCode(), de.getMessage()).toBuffer());
            return;
        }
        //otherwise
        c
                .response()
                .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .send(error(500).toBuffer());
    };

    //region common error handler
    Handler<RoutingContext> DEFUALT_401_HANDLER = c -> c
            .response()
            .setStatusCode(HttpResponseStatus.UNAUTHORIZED.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            .send(error(401, "登录过期或未登录，请登录后进行操作！", DomainError.MODE_FATAL).toBuffer());
    Handler<RoutingContext> DEFUALT_403_HANDLER = c -> c
            .response()
            .setStatusCode(HttpResponseStatus.FORBIDDEN.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            .send(error(403, "无操作权限！", DomainError.MODE_PROMPT).toBuffer());
    Handler<RoutingContext> DEFUALT_404_HANDLER = c -> c
            .response()
            .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            .send(error(404, null, DomainError.MODE_NOTIFY).toBuffer());
    Handler<RoutingContext> DEFUALT_409_HANDLER = c -> c
            .response()
            .setStatusCode(HttpResponseStatus.CONFLICT.code())
            .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
            .send(error(409, "数据冲突，请刷新后重试操作！", DomainError.MODE_FATAL).toBuffer());
    Map<Integer, Handler<RoutingContext>> ERROR_HANDLER_REGISTRY = Stream.of(
                    Map.entry(500, DEFAULT_500_HANDLER)
                    , Map.entry(401, DEFUALT_401_HANDLER)
                    , Map.entry(404, DEFUALT_404_HANDLER)
                    , Map.entry(403, DEFUALT_403_HANDLER)
                    , Map.entry(409, DEFUALT_409_HANDLER)
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    static JsonObject error(int status) {
        return error(status, null, 0);
    }

    static JsonObject error(int status, @Nullable String message) {
        return error(status, message, 0);
    }

    static JsonObject error(int status, @Nullable String message, int prompt) {
        var j = JsonObject.of(
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "code", status
        );
        if (message != null && !message.isBlank()) {
            j.put("message", message);
        }
        if (prompt > 0) {
            j.put("mode", prompt);
        }
        return j;
    }

    static JsonObject ok(JsonObject data, @Nullable String message, int prompt) {
        var j = JsonObject.of(
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "code", 200,
                "data", data
        );
        if (message != null && !message.isBlank()) {
            j.put("message", message);
        }
        if (prompt > 0) {
            j.put("mode", prompt);
        }
        return j;
    }

    static JsonObject ok(JsonObject data, @Nullable String message) {
        return ok(data, message, 0);
    }

    static JsonObject ok(JsonObject data) {
        return ok(data, null, 0);
    }

    static JsonObject ok(JsonArray data, @Nullable String message, int prompt) {
        var j = JsonObject.of(
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "code", 200,
                "data", data
        );
        if (message != null && !message.isBlank()) {
            j.put("message", message);
        }
        if (prompt > 0) {
            j.put("mode", prompt);
        }
        return j;
    }

    static JsonObject ok(JsonArray data, @Nullable String message) {
        return ok(data, message, 0);
    }

    static JsonObject ok(JsonArray data) {
        return ok(data, null, 0);
    }

    static JsonObject ok(String data, @Nullable String message, int prompt) {
        var j = JsonObject.of(
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "code", 200,
                "data", data
        );
        if (message != null && !message.isBlank()) {
            j.put("message", message);
        }
        if (prompt > 0) {
            j.put("mode", prompt);
        }
        return j;
    }

    static JsonObject ok(String data, @Nullable String message) {
        return ok(data, message, 0);
    }

    static JsonObject ok(String data) {
        return ok(data, null, 0);
    }

    static JsonObject ok(Number data, @Nullable String message, int prompt) {
        var j = JsonObject.of(
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "code", 200,
                "data", data instanceof Long l ? String.valueOf(l) : data
        );
        if (message != null && !message.isBlank()) {
            j.put("message", message);
        }
        if (prompt > 0) {
            j.put("mode", prompt);
        }
        return j;
    }

    static JsonObject ok(Number data, @Nullable String message) {
        return ok(data, message, 0);
    }

    static JsonObject ok(Number data) {
        return ok(data, null, 0);
    }

    //endregion
    static void register(Router router) {
        ERROR_HANDLER_REGISTRY.forEach(router::errorHandler);
    }

    Buffer EMPTY = BufferInternal.buffer(Unpooled.EMPTY_BUFFER);

    interface EventEmitter {
        HttpServerResponse $it();

        /**
         * event for SSE, each field can only set once
         */
        interface Event {
            Buffer $it();

            Event id(String evtId);

            Event event(String type);

            Event retry(int millsSeconds);

            Event data(String data);

            Event data(JsonObject data);

            Event data(JsonArray data);

            class evt implements Event {

                private final Buffer b = Buffer.buffer();

                public Buffer $it() {
                    return b;
                }

                private int s = 0;
                static final int FLAG_STATUS_ID = 1;
                static final int FLAG_STATUS_RETRY = 1 << 2;
                static final int FLAG_STATUS_EVENT = 1 << 3;
                static final int FLAG_STATUS_DATA = 1 << 4;
                static final byte[] FIELD_ID = "id:".getBytes(StandardCharsets.UTF_8);
                static final byte[] FIELD_EVENT = "event:".getBytes(StandardCharsets.UTF_8);
                static final byte[] FIELD_RETRY = "retry:".getBytes(StandardCharsets.UTF_8);
                static final byte[] FIELD_data = "data:".getBytes(StandardCharsets.UTF_8);

                @Override
                public Event id(String evtId) {
                    if ((s & FLAG_STATUS_ID) != 0) throw new IllegalStateException("ID already set");
                    b.appendBytes(FIELD_ID).appendString(evtId).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_ID;
                    return this;
                }

                @Override
                public Event event(String type) {
                    if ((s & FLAG_STATUS_EVENT) != 0) throw new IllegalStateException("Event already set");
                    b.appendBytes(FIELD_EVENT).appendString(type).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_EVENT;
                    return this;
                }

                @Override
                public Event retry(int millsSeconds) {
                    if ((s & FLAG_STATUS_RETRY) != 0) throw new IllegalStateException("Retry already set");
                    b.appendBytes(FIELD_RETRY).appendString("" + millsSeconds).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_RETRY;
                    return this;
                }

                @Override
                public Event data(String data) {
                    if ((s & FLAG_STATUS_DATA) != 0) throw new IllegalStateException("Data already set");
                    b.appendBytes(FIELD_data).appendString(data).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_DATA;
                    return this;

                }

                @Override
                public Event data(JsonObject data) {
                    if ((s & FLAG_STATUS_DATA) != 0) throw new IllegalStateException("Data already set");
                    b.appendBytes(FIELD_data).appendBuffer(data.toBuffer()).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_DATA;
                    return this;
                }

                @Override
                public Event data(JsonArray data) {
                    if ((s & FLAG_STATUS_DATA) != 0) throw new IllegalStateException("Data already set");
                    b.appendBytes(FIELD_data).appendBuffer(data.toBuffer()).appendByte((byte) '\n');
                    s = s | FLAG_STATUS_DATA;
                    return this;
                }
            }
        }


        default EventEmitter $write(Buffer value) {
            var r = $it();
            r.write(value);
            return this;
        }

        default EventEmitter send(Event event) {
            var b = event.$it();
            b.appendByte((byte) '\n');
            $write(b);
            return this;
        }

        default EventEmitter sendData(String value) {
            $write(Buffer.buffer("data:")
                    .appendString(value)
                    .appendString("\n\n"));
            return this;
        }

        default EventEmitter sendData(JsonObject value) {
            $write(Buffer.buffer("data:")
                    .appendString(value.encode())
                    .appendString("\n\n"));
            return this;
        }

        default EventEmitter send(UnaryOperator<Event> eventMaker) {
            var event = eventMaker.apply(new Event.evt());
            var b = event.$it();
            b.appendByte((byte) '\n');
            $write(b);
            return this;
        }

        default EventEmitter send(Consumer<Event> eventMaker) {
            var event = new Event.evt();
            eventMaker.accept(event);
            var b = event.$it();
            b.appendByte((byte) '\n');
            $write(b);
            return this;
        }

        default EventEmitter open() {
            return $write(Buffer.buffer("event:open\n\n"));
        }

        default boolean closed() {
            return $it().ended() || $it().closed();
        }

        default void close() {
            if (!closed())
                $it().end();
        }

    }
}
