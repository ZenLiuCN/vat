package vat.api.implement;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SecurityAuditLoggerHandler;
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
import vat.api.utils.Pointer;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.*;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface Web extends Router, Applicative<Web> {
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
        default <T extends Data> Handler<AsyncResult<T>> respondOne() {
            return BaseHandlers.sendOne(debug(), raw());
        }

        /// send  converted json data for js compatible
        default <T extends Data> Handler<AsyncResult<T>> respondJs() {
            return BaseHandlers.sendJsonData(debug(), raw());
        }

        /// send  converted json data list for js compatible
        default <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> respondJsList() {
            return BaseHandlers.sendJsonDataList(debug(), raw());
        }

        /// send none converted json data
        default <T extends Data> Handler<AsyncResult<T>> respondJson() {
            return BaseHandlers.sendJsonDataJson(debug(), raw());
        }

        /// send none converted json data list
        default <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> respondJsonList() {
            return BaseHandlers.sendJsonDataJsonList(debug(), raw());
        }

        /// send raw json data list
        default Handler<AsyncResult<JsonObject>> respond() {
            return BaseHandlers.sendJson(debug(), raw());
        }

        default <T> Handler<AsyncResult<T>> respond(BiFunction<Context, T, Future<Void>> response) {
            return BaseHandlers.response(this, response);
        }

        /// send raw json list
        default Handler<AsyncResult<JsonArray>> respondList() {
            return BaseHandlers.sendJsonArray(debug(), raw());
        }

        /// send nothing
        default Handler<AsyncResult<Void>> respondVoid() {
            return BaseHandlers.sendVoid(debug(), raw());
        }

        /// send raw text
        default Handler<AsyncResult<String>> respondText() {
            return BaseHandlers.sendText(debug(), raw());
        }

        default Handler<AsyncResult<String>> respondHtmlText() {
            return BaseHandlers.sendHtmlText(debug(), raw());
        }

        default Handler<AsyncResult<Buffer>> respondHtmlBuffer() {
            return BaseHandlers.sendHtmlBuffer(debug(), raw());
        }


        //Response send with data container
        default <T extends Data> Handler<AsyncResult<T>> respondJsData() {
            return BaseHandlers.sendJsonDataContainer(debug(), raw());
        }

        default <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> respondJsListData() {
            return BaseHandlers.sendJsonDataListContainer(debug(), raw());
        }

        default <T extends Data> Handler<AsyncResult<T>> respondJsonData() {
            return BaseHandlers.sendJsonDataJsonContainer(debug(), raw());
        }

        default <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> respondJsonListData() {
            return BaseHandlers.sendJsonDataJsonListContainer(debug(), raw());
        }


        default Handler<AsyncResult<JsonObject>> respondData() {
            return BaseHandlers.sendContainer(debug(), raw());
        }

        default Handler<AsyncResult<JsonArray>> respondListData() {
            return BaseHandlers.sendArrayContainer(debug(), raw());
        }

        default Handler<AsyncResult<Void>> respondVoidData() {
            return BaseHandlers.sendVoidContainer(debug(), raw());
        }

        default Handler<AsyncResult<String>> respondTextData() {
            return BaseHandlers.sendTextContainer(debug(), raw());
        }

        default Handler<AsyncResult<String>> redirect() {
            return BaseHandlers.redirectTo(debug(), raw());
        }

        default Handler<AsyncResult<Buffer>> binary(String contentType, @Nullable String fileName) {
            return BaseHandlers.sendBinary(raw(), contentType, fileName, null);
        }

        default <T> Handler<AsyncResult<T>> binary(Function<T, Buffer> binary,
                                                   Function<T, String> contentType,
                                                   Function<T, String> fileName) {
            return r -> {
                if (r.succeeded()) {
                    var res = r.result();
                    BaseHandlers.sendBinary(raw(), contentType.apply(res), fileName == null ? null : fileName.apply(res), null)
                            .handle(r.map(binary));
                } else {
                    BaseHandlers.sendBinary(raw(), null, null, null).handle(r.mapEmpty());
                }
            };
        }
        //endregion


        //region Error
        default void info(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(BaseHandlers.error(status, message, DomainError.MODE_NOTIFY));
        }

        default void error(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(BaseHandlers.error(status, message, DomainError.MODE_PROMPT));
        }

        default void fatal(int status, @Nullable String message) {
            raw().response().setStatusCode(status);
            raw().json(BaseHandlers.error(status, message, DomainError.MODE_FATAL));
        }

        default void withInfo(JsonArray data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_NOTIFY));
        }

        default void withInfo(Number data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_NOTIFY));
        }

        default void withInfo(JsonObject data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_NOTIFY));
        }


        default void withError(JsonArray data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_PROMPT));
        }

        default void withError(Number data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_PROMPT));
        }

        default void withError(JsonObject data, @Nullable String message) {
            raw().json(BaseHandlers.ok(data, message, DomainError.MODE_PROMPT));
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
}
