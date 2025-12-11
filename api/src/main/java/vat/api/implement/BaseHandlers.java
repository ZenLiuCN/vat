package vat.api.implement;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.buffer.BufferInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.meta.Nullable;
import vat.api.utils.Fn;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vat.api.implement.Web.log;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface BaseHandlers {
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

    static void register(Router router) {
        ERROR_HANDLER_REGISTRY.forEach(router::errorHandler);
    }

    static Handler<AsyncResult<String>> sendText(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                setHeader(ctx, HttpHeaderValues.TEXT_PLAIN).end(r.result());
            } else {
                ctx.fail(r.cause());
            }
        };
    }

    static Handler<AsyncResult<String>> sendHtmlText(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                setHeader(ctx, HttpHeaderValues.TEXT_HTML).end(r.result());
            } else {
                ctx.fail(r.cause());
            }
        };
    }

    static Handler<AsyncResult<Buffer>> sendHtmlBuffer(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                setHeader(ctx, HttpHeaderValues.TEXT_HTML).end(r.result());
            } else {
                ctx.fail(r.cause());
            }
        };
    }

    static HttpServerResponse setHeader(RoutingContext ctx, CharSequence contentType) {
        var res = ctx.response();
        if (res.headers().contains(HttpHeaders.CONTENT_TYPE) || contentType == null) return res;
        res.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        return res;
    }


    static <T extends Data> Handler<AsyncResult<T>> sendJsonDataJson(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                ctx.json(toJson(r.result()));
            } else {
                ctx.fail(r.cause());
            }
        };
    }

    static <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> sendJsonDataJsonList(
            boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                ctx.json(toJson(r.result()));
            } else {
                ctx.fail(r.cause());
            }
        };
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

    static <T extends Data> Handler<AsyncResult<T>> sendOne(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                ctx.json(toJson(r.result()));
            } else {
                if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                ctx.fail(r.cause());
            }
        };
    }

    static <T extends Data> Handler<AsyncResult<T>> sendJsonData(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                ctx.json(toJS(r.result()));
            } else {
                if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                ctx.fail(r.cause());
            }
        };
    }

    static <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> sendJsonDataList(boolean debug, RoutingContext ctx) {
        return r -> {
            if (r.succeeded()) {
                ctx.json(toJS(r.result()));
            } else {
                if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                ctx.fail(r.cause());
            }
        };
    }

    static <T extends Data> Handler<AsyncResult<T>> sendJsonDataContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", toJS(r.result())
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception ex) {
                if (debug) log.error("fail {}", Web.dump(ctx), ex);
                ctx.fail(ex);
            }
        };
    }

    static <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> sendJsonDataListContainer(
            boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", toJS(r.result())
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static <T extends Data> Handler<AsyncResult<T>> sendJsonDataJsonContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", toJson(r.result())
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception ex) {
                if (debug) log.error("fail {}", Web.dump(ctx), ex);
                ctx.fail(ex);
            }
        };
    }

    static <T extends Data, R extends Collection<T>> Handler<AsyncResult<R>> sendJsonDataJsonListContainer(
            boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", toJson(r.result())
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<Void>> sendVoidContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonObject>> sendContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", r.result()
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<String>> sendTextContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", r.result()
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonArray>> sendArrayContainer(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(JsonObject.of(
                            "code", 200,
                            "data", r.result()
                    ));
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static <T> Handler<AsyncResult<T>> response(Web.Context ctx,
                                                BiFunction<Web.Context, T, Future<Void>> response) {
        return r -> {
            try {
                if (r.succeeded()) {
                    try {
                        var f = response.apply(ctx, r.result());
                        if (f != null) {
                            f.onFailure(ctx::fail);
                        }
                    } catch (Exception e) {
                        ctx.fail(e);
                    }
                } else {
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonObject>> sendJson(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(r.result());
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonArray>> sendJsonArray(boolean debug, RoutingContext ctx) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(r.result());
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonObject>> sendJson(RoutingContext ctx,
                                                     BiConsumer<RoutingContext, Throwable> errorHandler) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(r.result());
                } else if (errorHandler != null) {
                    errorHandler.accept(ctx, r.cause());
                } else {
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                ctx.fail(e);
            }
        };
    }

    static Handler<AsyncResult<JsonArray>> sendJsonArray(RoutingContext ctx,
                                                         BiConsumer<RoutingContext, Throwable> errorHandler) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.json(r.result());
                } else if (errorHandler != null) {
                    errorHandler.accept(ctx, r.cause());
                } else {
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                ctx.fail(e);
            }
        };
    }

    static <T> Handler<AsyncResult<T>> sendVoid(boolean debug, RoutingContext ctx,
                                                BiConsumer<RoutingContext, Throwable> errorHandler) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.end("");
                } else if (errorHandler != null) {
                    errorHandler.accept(ctx, r.cause());
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }

    static <T> Handler<AsyncResult<T>> sendVoid(boolean debug, RoutingContext ctx) {
        return sendVoid(debug, ctx, null);
    }

    static Handler<AsyncResult<String>> redirectTo(boolean debug, RoutingContext ctx) {
        return redirectTo(debug, ctx, null);
    }

    static Handler<AsyncResult<String>> redirectTo(boolean debug, RoutingContext ctx,
                                                   BiConsumer<RoutingContext, Throwable> errorHandler) {
        return r -> {
            try {
                if (r.succeeded()) {
                    ctx.redirect(r.result())
                            .onComplete(v -> {
                                if (v.failed()) {
                                    if (errorHandler != null) errorHandler.accept(ctx, r.cause());
                                    else {
                                        log.error("redirect to failure", v.cause());
                                        ctx.fail(v.cause());
                                    }
                                }
                            });
                } else if (errorHandler != null) {
                    errorHandler.accept(ctx, r.cause());
                } else {
                    if (debug) log.error("fail {}", Web.dump(ctx), r.cause());
                    ctx.fail(r.cause());
                }
            } catch (Exception e) {
                if (debug) log.error("fail {}", Web.dump(ctx), e);
                ctx.fail(e);
            }
        };
    }


    static JsonObject parameters(RoutingContext cx) {
        var j = new JsonObject();
        cx.pathParams().forEach(j::put);
        cx.queryParams().forEach(j::put);
        return j;
    }


    static Handler<AsyncResult<Buffer>> sendBinary(RoutingContext cx, String contentType, @Nullable String file,
                                                   @Nullable BiConsumer<RoutingContext, Throwable> errorHandler) {
        return r -> {
            try {
                if (r.succeeded())
                    try {
                        var res = setHeader(cx,
                                Fn.notBlank(contentType) ? contentType : HttpHeaderValues.APPLICATION_OCTET_STREAM);
                        if (Fn.notBlank(file)) {
                            res.putHeader(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment;filename*=UTF-8''" + URLEncoder.encode(file, StandardCharsets.UTF_8));
                        }
                        res.setStatusCode(200);
                        res.send(r.result());
                    } catch (Exception e) {
                        if (errorHandler != null) {
                            errorHandler.accept(cx, e);
                        } else {
                            cx.fail(e);
                        }
                    }
                else if (errorHandler != null) {
                    errorHandler.accept(cx, r.cause());
                } else {
                    cx.fail(r.cause());
                }
            } catch (Exception e) {
                cx.fail(e);
            }
        };
    }

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

    Buffer EMPTY = BufferInternal.buffer(Unpooled.EMPTY_BUFFER);

    static <C extends RoutingContext, R> void sse(C c, Function<EventEmitter, R> onConnect, Handler<R> onFinish) {
        var res = c.response();
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
            if (!res.ended()) {
                res.end();
            }
        }).endHandler($ -> {
            onFinish.handle(r);
            if (!res.ended()) {
                res.end();
            }
        });
    }

    static <C extends RoutingContext, R> void sse(C c, BiFunction<C, EventEmitter, R> onConnect, Handler<R> onFinish) {
        var res = c.response();
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
        var r = onConnect.apply(c, () -> res);
        res.closeHandler($ -> {
            onFinish.handle(r);
            if (!res.ended()) {
                res.end();
            }
        }).endHandler($ -> {
            onFinish.handle(r);
            if (!res.ended()) {
                res.end();
            }
        });
    }


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

}
