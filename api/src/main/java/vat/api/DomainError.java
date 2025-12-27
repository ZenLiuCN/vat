package vat.api;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullUnmarked;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;


/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
@NullUnmarked
public class DomainError extends RuntimeException {
    public static final Supplier<DomainError> BAD_REQUEST_SYSTEM = () -> DomainError.System.badRequest(
            "argument required");
    public static final Function<String, Supplier<DomainError>> MISSING_CONFIG_SYSTEM = (prop) -> () -> DomainError.System.badRequest(
            "config property " + prop + " required");
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_PAYMENT_REQUIRED = 402;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_METHOD_NOT_ALLOWED = 405;
    public static final int CODE_NOT_ACCEPTABLE = 406;
    public static final int CODE_REQUEST_TIMEOUT = 408;
    public static final int CODE_CONFLICT = 409;
    public static final int CODE_GONE = 410;
    public static final int CODE_PRECONDITION_FAILED = 412;
    public static final int CODE_UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int CODE_UNSUPPORTED_TYPE = 416;
    public static final int CODE_TOO_MANY_REQUESTS = 429;
    public static final int CODE_UNAVAILABLE_FOR_LEGAL_REASONS = 451;
    public static final int CODE_INTERNAL_SERVER_ERROR = 500;
    public static final int CODE_NOT_IMPLEMENTED = 501;
    public static final int CODE_BAD_GATEWAY = 502;
    public static final int CODE_SERVICE_UNAVAILABLE = 503;
    public static final int CODE_GATEWAY_TIMEOUT = 504;
    public static final int CODE_NETWORK_AUTHENTICATION_REQUIRED = 505;
    public static final AtomicInteger STACK_TRACE_SIZE = new AtomicInteger(1024);
    public static final AtomicInteger STACK_TRACE_LINES = new AtomicInteger(50);
    /// 无提示
    public static final int MODE_MUTE = 0;
    /// 弱提醒(例如: toast)
    public static final int MODE_NOTIFY = 1;
    /// 强提醒(例如: 确认窗口 )
    public static final int MODE_PROMPT = 2;
    /// 严重错误(按需处理)
    public static final int MODE_FATAL = 3;
    public final int code;
    public final String user;
    public final int mode;

    public DomainError(int mode, int code, String user, String message) {
        this(mode, code, user, message == null ? user : message, null, false);

    }


    public DomainError(int mode, int code, String user, String message, Throwable cause) {
        this(mode, code, user, message == null ? user : message, cause, false);
    }

    protected DomainError(int mode, int code, String user, String message, Throwable cause, boolean noStackTrace) {
        super(message == null ? user : message, cause, !noStackTrace, !noStackTrace);
        this.code = code;
        this.user = user;
        this.mode = mode;
    }

    protected transient JsonArray stacktrace;

    public static JsonArray dumpStack(Throwable cause) {
        return cause != null && cause.getStackTrace() != null
                ? Arrays.stream(cause.getStackTrace()).map(StackTraceElement::toString).limit(STACK_TRACE_LINES.get())
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
                : new JsonArray();
    }

    public static JsonObject dumpJsonObject(Throwable ex) {
        if (ex instanceof DomainError e) {
            return e.asJson();
        }
        return JsonObject.of()
                         .put("type", ex.getClass().getCanonicalName())
                         .put("message", ex.getMessage())
                         .put("stackTrace", dumpStack(ex))
                ;
    }

    public JsonArray stacktrace() {
        if (this.stacktrace == null) {
            this.stacktrace = dumpStack(this);
        }
        return this.stacktrace;
    }

    public DomainError(JsonObject data) {
        super(data.getString("message", ""));
        code = data.getInteger("code", CODE_INTERNAL_SERVER_ERROR);
        user = data.getString("user", "");
        mode = data.getInteger("mode", 0);
        stacktrace = data.getJsonArray("stackTrace");
    }

    public JsonObject asJson() {
        if (this.stacktrace == null) {
            this.stacktrace = dumpStack(this);
        }
        return new JsonObject()
                .put("code", code)
                .put("user", user)
                .put("mode", mode)
                .put("message", getMessage())
                .put("stackTrace", stacktrace);

    }

    static class Builder {
        private String message;
        private int code;
        private String user;
        private int mode;
        private Throwable cause;
        private boolean noStackTrace;

        public Builder user(int mode, String pattern, Object... args) {
            this.mode = mode;
            if (args.length == 0) {
                this.user = pattern;
            } else {
                var e = MessageFormatter.arrayFormat(pattern, args);
                if (e.getThrowable() != null) {
                    this.cause = e.getThrowable();
                }
                this.user = e.getMessage();
            }
            return this;
        }

        public Builder system(String pattern, Object... args) {
            if (args.length == 0) {
                this.message = pattern;
            } else {
                var e = MessageFormatter.arrayFormat(pattern, args);
                if (e.getThrowable() != null) {
                    this.cause = e.getThrowable();
                }
                this.message = e.getMessage();
            }
            return this;
        }

        public Builder noStackTrace() {
            this.noStackTrace = true;
            return this;
        }

        public Builder stackTrace() {
            this.noStackTrace = false;
            return this;
        }

        public DomainError build() {
            return new DomainError(mode, code, user, message, cause, noStackTrace);
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder badRequest() {
            this.code = CODE_BAD_REQUEST;
            return this;
        }

        public Builder unauthorized() {
            this.code = CODE_UNAUTHORIZED;
            return this;
        }

        public Builder paymentRequired() {
            this.code = CODE_PAYMENT_REQUIRED;
            return this;
        }

        public Builder forbidden() {
            this.code = CODE_FORBIDDEN;
            return this;
        }

        public Builder notFound() {
            this.code = CODE_NOT_FOUND;
            return this;
        }

        public Builder methodNotAllowed() {
            this.code = CODE_METHOD_NOT_ALLOWED;
            return this;
        }

        public Builder notAcceptable() {
            this.code = CODE_NOT_ACCEPTABLE;
            return this;
        }

        public Builder requestTimeout() {
            this.code = CODE_REQUEST_TIMEOUT;
            return this;
        }

        public Builder conflict() {
            this.code = CODE_CONFLICT;
            return this;
        }

        public Builder gone() {
            this.code = CODE_GONE;
            return this;
        }

        public Builder preconditionFailed() {
            this.code = CODE_PRECONDITION_FAILED;
            return this;
        }

        public Builder unsupportedMediaType() {
            this.code = CODE_UNSUPPORTED_MEDIA_TYPE;
            return this;
        }

        public Builder unsupportedType() {
            this.code = CODE_UNSUPPORTED_TYPE;
            return this;
        }

        public Builder tooManyRequests() {
            this.code = CODE_TOO_MANY_REQUESTS;
            return this;
        }

        public Builder unavailableForLegalReasons() {
            this.code = CODE_UNAVAILABLE_FOR_LEGAL_REASONS;
            return this;
        }

        public Builder internalServerError() {
            this.code = CODE_INTERNAL_SERVER_ERROR;
            return this;
        }

        public Builder notImplemented() {
            this.code = CODE_NOT_IMPLEMENTED;
            return this;
        }

        public Builder badGateway() {
            this.code = CODE_BAD_GATEWAY;
            return this;
        }

        public Builder serviceUnavailable() {
            this.code = CODE_SERVICE_UNAVAILABLE;
            return this;
        }

        public Builder gatewayTimeout() {
            this.code = CODE_GATEWAY_TIMEOUT;
            return this;
        }

        public Builder networkAuthenticationRequired() {
            this.code = CODE_NETWORK_AUTHENTICATION_REQUIRED;
            return this;
        }

    }


    public interface System {
        static DomainError badRequest(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_BAD_REQUEST, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_BAD_REQUEST, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_BAD_REQUEST, null, e.getMessage());
        }

        static DomainError unauthorized(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_UNAUTHORIZED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_UNAUTHORIZED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_UNAUTHORIZED, null, e.getMessage());
        }

        static DomainError paymentRequired(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_PAYMENT_REQUIRED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_PAYMENT_REQUIRED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_PAYMENT_REQUIRED, null, e.getMessage());
        }

        static DomainError forbidden(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_FORBIDDEN, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_FORBIDDEN, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_FORBIDDEN, null, e.getMessage());
        }

        static DomainError notFound(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_NOT_FOUND, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_NOT_FOUND, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_NOT_FOUND, null, e.getMessage());
        }

        static DomainError methodNotAllowed(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_METHOD_NOT_ALLOWED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_METHOD_NOT_ALLOWED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_METHOD_NOT_ALLOWED, null, e.getMessage());
        }

        static DomainError notAcceptable(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_NOT_ACCEPTABLE, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_NOT_ACCEPTABLE, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_NOT_ACCEPTABLE, null, e.getMessage());
        }

        static DomainError requestTimeout(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_REQUEST_TIMEOUT, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_REQUEST_TIMEOUT, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_REQUEST_TIMEOUT, null, e.getMessage());
        }

        static DomainError conflict(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_CONFLICT, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_CONFLICT, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_CONFLICT, null, e.getMessage());
        }

        static DomainError gone(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_GONE, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_GONE, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_GONE, null, e.getMessage());
        }

        static DomainError preconditionFailed(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_PRECONDITION_FAILED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_PRECONDITION_FAILED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_PRECONDITION_FAILED, null, e.getMessage());
        }

        static DomainError unsupportedMediaType(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_UNSUPPORTED_MEDIA_TYPE, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_UNSUPPORTED_MEDIA_TYPE, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_UNSUPPORTED_MEDIA_TYPE, null, e.getMessage());
        }

        static DomainError unsupportedType(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_UNSUPPORTED_TYPE, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_UNSUPPORTED_TYPE, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_UNSUPPORTED_TYPE, null, e.getMessage());
        }

        static DomainError tooManyRequests(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_TOO_MANY_REQUESTS, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_TOO_MANY_REQUESTS, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_TOO_MANY_REQUESTS, null, e.getMessage());
        }

        static DomainError unavailableForLegalReasons(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, null, e.getMessage());
        }

        static DomainError internalServerError(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_INTERNAL_SERVER_ERROR, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_INTERNAL_SERVER_ERROR, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_INTERNAL_SERVER_ERROR, null, e.getMessage());
        }

        static DomainError notImplemented(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_NOT_IMPLEMENTED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_NOT_IMPLEMENTED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_NOT_IMPLEMENTED, null, e.getMessage());
        }

        static DomainError badGateway(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_BAD_GATEWAY, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_BAD_GATEWAY, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_BAD_GATEWAY, null, e.getMessage());
        }

        static DomainError serviceUnavailable(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_SERVICE_UNAVAILABLE, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_SERVICE_UNAVAILABLE, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_SERVICE_UNAVAILABLE, null, e.getMessage());
        }

        static DomainError gatewayTimeout(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_GATEWAY_TIMEOUT, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_GATEWAY_TIMEOUT, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_GATEWAY_TIMEOUT, null, e.getMessage());
        }

        static DomainError networkAuthenticationRequired(String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(0, CODE_NETWORK_AUTHENTICATION_REQUIRED, null, pattern);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(0, CODE_NETWORK_AUTHENTICATION_REQUIRED, null, e.getMessage(), e.getThrowable());
            }
            return new DomainError(0, CODE_NETWORK_AUTHENTICATION_REQUIRED, null, e.getMessage());
        }


    }

    public interface User {
        static DomainError badRequest(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_BAD_REQUEST, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_BAD_REQUEST, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_BAD_REQUEST, e.getMessage(), null);
        }

        static DomainError unauthorized(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_UNAUTHORIZED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_UNAUTHORIZED, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_UNAUTHORIZED, e.getMessage(), null);
        }

        static DomainError paymentRequired(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_PAYMENT_REQUIRED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_PAYMENT_REQUIRED, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_PAYMENT_REQUIRED, e.getMessage(), null);
        }

        static DomainError forbidden(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_FORBIDDEN, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_FORBIDDEN, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_FORBIDDEN, e.getMessage(), null);
        }

        static DomainError notFound(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_NOT_FOUND, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_NOT_FOUND, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_NOT_FOUND, e.getMessage(), null);
        }

        static DomainError methodNotAllowed(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_METHOD_NOT_ALLOWED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_METHOD_NOT_ALLOWED, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_METHOD_NOT_ALLOWED, e.getMessage(), null);
        }

        static DomainError notAcceptable(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_NOT_ACCEPTABLE, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_NOT_ACCEPTABLE, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_NOT_ACCEPTABLE, e.getMessage(), null);
        }

        static DomainError requestTimeout(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_REQUEST_TIMEOUT, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_REQUEST_TIMEOUT, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_REQUEST_TIMEOUT, e.getMessage(), null);
        }

        static DomainError conflict(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_CONFLICT, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_CONFLICT, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_CONFLICT, e.getMessage(), null);
        }

        static DomainError gone(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_GONE, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_GONE, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_GONE, e.getMessage(), null);
        }

        static DomainError preconditionFailed(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_PRECONDITION_FAILED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_PRECONDITION_FAILED, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_PRECONDITION_FAILED, e.getMessage(), null);
        }

        static DomainError unsupportedMediaType(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_UNSUPPORTED_MEDIA_TYPE, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_UNSUPPORTED_MEDIA_TYPE, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_UNSUPPORTED_MEDIA_TYPE, e.getMessage(), null);
        }

        static DomainError unsupportedType(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_UNSUPPORTED_TYPE, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_UNSUPPORTED_TYPE, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_UNSUPPORTED_TYPE, e.getMessage(), null);
        }

        static DomainError tooManyRequests(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_TOO_MANY_REQUESTS, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_TOO_MANY_REQUESTS, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_TOO_MANY_REQUESTS, e.getMessage(), null);
        }

        static DomainError unavailableForLegalReasons(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, e.getMessage(), null,
                                       e.getThrowable());
            }
            return new DomainError(mode, CODE_UNAVAILABLE_FOR_LEGAL_REASONS, e.getMessage(), null);
        }

        static DomainError internalServerError(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_INTERNAL_SERVER_ERROR, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_INTERNAL_SERVER_ERROR, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_INTERNAL_SERVER_ERROR, e.getMessage(), null);
        }

        static DomainError notImplemented(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_NOT_IMPLEMENTED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_NOT_IMPLEMENTED, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_NOT_IMPLEMENTED, e.getMessage(), null);
        }

        static DomainError badGateway(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_BAD_GATEWAY, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_BAD_GATEWAY, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_BAD_GATEWAY, e.getMessage(), null);
        }

        static DomainError serviceUnavailable(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_SERVICE_UNAVAILABLE, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_SERVICE_UNAVAILABLE, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_SERVICE_UNAVAILABLE, e.getMessage(), null);
        }

        static DomainError gatewayTimeout(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_GATEWAY_TIMEOUT, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_GATEWAY_TIMEOUT, e.getMessage(), null, e.getThrowable());
            }
            return new DomainError(mode, CODE_GATEWAY_TIMEOUT, e.getMessage(), null);
        }

        static DomainError networkAuthenticationRequired(int mode, String pattern, Object... args) {
            if (args.length == 0) {
                return new DomainError(mode, CODE_NETWORK_AUTHENTICATION_REQUIRED, pattern, null);
            }
            var e = MessageFormatter.arrayFormat(pattern, args);
            if (e.getThrowable() != null) {
                return new DomainError(mode, CODE_NETWORK_AUTHENTICATION_REQUIRED, e.getMessage(), null,
                                       e.getThrowable());
            }
            return new DomainError(mode, CODE_NETWORK_AUTHENTICATION_REQUIRED, e.getMessage(), null);
        }


        static DomainError badRequestMute(String pattern, Object... args) {
            return badRequest(0, pattern, args);
        }

        static DomainError badRequestNotify(String pattern, Object... args) {
            return badRequest(1, pattern, args);
        }

        static DomainError badRequestPrompt(String pattern, Object... args) {
            return badRequest(2, pattern, args);
        }

        static DomainError badRequestFatal(String pattern, Object... args) {
            return badRequest(3, pattern, args);
        }

        static DomainError unauthorizedMute(String pattern, Object... args) {
            return unauthorized(0, pattern, args);
        }

        static DomainError unauthorizedNotify(String pattern, Object... args) {
            return unauthorized(1, pattern, args);
        }

        static DomainError unauthorizedPrompt(String pattern, Object... args) {
            return unauthorized(2, pattern, args);
        }

        static DomainError unauthorizedFatal(String pattern, Object... args) {
            return unauthorized(3, pattern, args);
        }

        static DomainError paymentRequiredMute(String pattern, Object... args) {
            return paymentRequired(0, pattern, args);
        }

        static DomainError paymentRequiredNotify(String pattern, Object... args) {
            return paymentRequired(1, pattern, args);
        }

        static DomainError paymentRequiredPrompt(String pattern, Object... args) {
            return paymentRequired(2, pattern, args);
        }

        static DomainError paymentRequiredFatal(String pattern, Object... args) {
            return paymentRequired(3, pattern, args);
        }

        static DomainError forbiddenMute(String pattern, Object... args) {
            return forbidden(0, pattern, args);
        }

        static DomainError forbiddenNotify(String pattern, Object... args) {
            return forbidden(1, pattern, args);
        }

        static DomainError forbiddenPrompt(String pattern, Object... args) {
            return forbidden(2, pattern, args);
        }

        static DomainError forbiddenFatal(String pattern, Object... args) {
            return forbidden(3, pattern, args);
        }

        static DomainError notFoundMute(String pattern, Object... args) {
            return notFound(0, pattern, args);
        }

        static DomainError notFoundNotify(String pattern, Object... args) {
            return notFound(1, pattern, args);
        }

        static DomainError notFoundPrompt(String pattern, Object... args) {
            return notFound(2, pattern, args);
        }

        static DomainError notFoundFatal(String pattern, Object... args) {
            return notFound(3, pattern, args);
        }

        static DomainError methodNotAllowedMute(String pattern, Object... args) {
            return methodNotAllowed(0, pattern, args);
        }

        static DomainError methodNotAllowedNotify(String pattern, Object... args) {
            return methodNotAllowed(1, pattern, args);
        }

        static DomainError methodNotAllowedPrompt(String pattern, Object... args) {
            return methodNotAllowed(2, pattern, args);
        }

        static DomainError methodNotAllowedFatal(String pattern, Object... args) {
            return methodNotAllowed(3, pattern, args);
        }

        static DomainError notAcceptableMute(String pattern, Object... args) {
            return notAcceptable(0, pattern, args);
        }

        static DomainError notAcceptableNotify(String pattern, Object... args) {
            return notAcceptable(1, pattern, args);
        }

        static DomainError notAcceptablePrompt(String pattern, Object... args) {
            return notAcceptable(2, pattern, args);
        }

        static DomainError notAcceptableFatal(String pattern, Object... args) {
            return notAcceptable(3, pattern, args);
        }

        static DomainError requestTimeoutMute(String pattern, Object... args) {
            return requestTimeout(0, pattern, args);
        }

        static DomainError requestTimeoutNotify(String pattern, Object... args) {
            return requestTimeout(1, pattern, args);
        }

        static DomainError requestTimeoutPrompt(String pattern, Object... args) {
            return requestTimeout(2, pattern, args);
        }

        static DomainError requestTimeoutFatal(String pattern, Object... args) {
            return requestTimeout(3, pattern, args);
        }

        static DomainError conflictMute(String pattern, Object... args) {
            return conflict(0, pattern, args);
        }

        static DomainError conflictNotify(String pattern, Object... args) {
            return conflict(1, pattern, args);
        }

        static DomainError conflictPrompt(String pattern, Object... args) {
            return conflict(2, pattern, args);
        }

        static DomainError conflictFatal(String pattern, Object... args) {
            return conflict(3, pattern, args);
        }

        static DomainError goneMute(String pattern, Object... args) {
            return gone(0, pattern, args);
        }

        static DomainError goneNotify(String pattern, Object... args) {
            return gone(1, pattern, args);
        }

        static DomainError gonePrompt(String pattern, Object... args) {
            return gone(2, pattern, args);
        }

        static DomainError goneFatal(String pattern, Object... args) {
            return gone(3, pattern, args);
        }

        static DomainError preconditionFailedMute(String pattern, Object... args) {
            return preconditionFailed(0, pattern, args);
        }

        static DomainError preconditionFailedNotify(String pattern, Object... args) {
            return preconditionFailed(1, pattern, args);
        }

        static DomainError preconditionFailedPrompt(String pattern, Object... args) {
            return preconditionFailed(2, pattern, args);
        }

        static DomainError preconditionFailedFatal(String pattern, Object... args) {
            return preconditionFailed(3, pattern, args);
        }

        static DomainError unsupportedMediaTypeMute(String pattern, Object... args) {
            return unsupportedMediaType(0, pattern, args);
        }

        static DomainError unsupportedMediaTypeNotify(String pattern, Object... args) {
            return unsupportedMediaType(1, pattern, args);
        }

        static DomainError unsupportedMediaTypePrompt(String pattern, Object... args) {
            return unsupportedMediaType(2, pattern, args);
        }

        static DomainError unsupportedMediaTypeFatal(String pattern, Object... args) {
            return unsupportedMediaType(3, pattern, args);
        }

        static DomainError unsupportedTypeMute(String pattern, Object... args) {
            return unsupportedType(0, pattern, args);
        }

        static DomainError unsupportedTypeNotify(String pattern, Object... args) {
            return unsupportedType(1, pattern, args);
        }

        static DomainError unsupportedTypePrompt(String pattern, Object... args) {
            return unsupportedType(2, pattern, args);
        }

        static DomainError unsupportedTypeFatal(String pattern, Object... args) {
            return unsupportedType(3, pattern, args);
        }

        static DomainError tooManyRequestsMute(String pattern, Object... args) {
            return tooManyRequests(0, pattern, args);
        }

        static DomainError tooManyRequestsNotify(String pattern, Object... args) {
            return tooManyRequests(1, pattern, args);
        }

        static DomainError tooManyRequestsPrompt(String pattern, Object... args) {
            return tooManyRequests(2, pattern, args);
        }

        static DomainError tooManyRequestsFatal(String pattern, Object... args) {
            return tooManyRequests(3, pattern, args);
        }

        static DomainError unavailableForLegalReasonsMute(String pattern, Object... args) {
            return unavailableForLegalReasons(0, pattern, args);
        }

        static DomainError unavailableForLegalReasonsNotify(String pattern, Object... args) {
            return unavailableForLegalReasons(1, pattern, args);
        }

        static DomainError unavailableForLegalReasonsPrompt(String pattern, Object... args) {
            return unavailableForLegalReasons(2, pattern, args);
        }

        static DomainError unavailableForLegalReasonsFatal(String pattern, Object... args) {
            return unavailableForLegalReasons(3, pattern, args);
        }

        static DomainError internalServerErrorMute(String pattern, Object... args) {
            return internalServerError(0, pattern, args);
        }

        static DomainError internalServerErrorNotify(String pattern, Object... args) {
            return internalServerError(1, pattern, args);
        }

        static DomainError internalServerErrorPrompt(String pattern, Object... args) {
            return internalServerError(2, pattern, args);
        }

        static DomainError internalServerErrorFatal(String pattern, Object... args) {
            return internalServerError(3, pattern, args);
        }

        static DomainError notImplementedMute(String pattern, Object... args) {
            return notImplemented(0, pattern, args);
        }

        static DomainError notImplementedNotify(String pattern, Object... args) {
            return notImplemented(1, pattern, args);
        }

        static DomainError notImplementedPrompt(String pattern, Object... args) {
            return notImplemented(2, pattern, args);
        }

        static DomainError notImplementedFatal(String pattern, Object... args) {
            return notImplemented(3, pattern, args);
        }

        static DomainError badGatewayMute(String pattern, Object... args) {
            return badGateway(0, pattern, args);
        }

        static DomainError badGatewayNotify(String pattern, Object... args) {
            return badGateway(1, pattern, args);
        }

        static DomainError badGatewayPrompt(String pattern, Object... args) {
            return badGateway(2, pattern, args);
        }

        static DomainError badGatewayFatal(String pattern, Object... args) {
            return badGateway(3, pattern, args);
        }

        static DomainError serviceUnavailableMute(String pattern, Object... args) {
            return serviceUnavailable(0, pattern, args);
        }

        static DomainError serviceUnavailableNotify(String pattern, Object... args) {
            return serviceUnavailable(1, pattern, args);
        }

        static DomainError serviceUnavailablePrompt(String pattern, Object... args) {
            return serviceUnavailable(2, pattern, args);
        }

        static DomainError serviceUnavailableFatal(String pattern, Object... args) {
            return serviceUnavailable(3, pattern, args);
        }

        static DomainError gatewayTimeoutMute(String pattern, Object... args) {
            return gatewayTimeout(0, pattern, args);
        }

        static DomainError gatewayTimeoutNotify(String pattern, Object... args) {
            return gatewayTimeout(1, pattern, args);
        }

        static DomainError gatewayTimeoutPrompt(String pattern, Object... args) {
            return gatewayTimeout(2, pattern, args);
        }

        static DomainError gatewayTimeoutFatal(String pattern, Object... args) {
            return gatewayTimeout(3, pattern, args);
        }

        static DomainError networkAuthenticationRequiredMute(String pattern, Object... args) {
            return networkAuthenticationRequired(0, pattern, args);
        }

        static DomainError networkAuthenticationRequiredNotify(String pattern, Object... args) {
            return networkAuthenticationRequired(1, pattern, args);
        }

        static DomainError networkAuthenticationRequiredPrompt(String pattern, Object... args) {
            return networkAuthenticationRequired(2, pattern, args);
        }

        static DomainError networkAuthenticationRequiredFatal(String pattern, Object... args) {
            return networkAuthenticationRequired(3, pattern, args);
        }


    }

    static Builder builder() {
        return new Builder();
    }

    public interface ErrorMaker {
        DomainError make(Object... args);
    }

    public record ErrorTuple(int mode, int code, String user, String system, int argument) implements ErrorMaker {
        public ErrorTuple(int mode, int code, String user, String system) {
            this(mode, code, user, system, gatherArgument(user, system));
        }

        static int gatherArgument(String user, String system) {
            var n = 0;
            if (user != null && user.contains("{}")) {
                n = user.split(Pattern.quote("{}")).length;
            }
            if (system != null && system.contains("{}")) {
                var sn = system.split(Pattern.quote("{}")).length;
                if (sn == n) return n;
                return Math.max(n, sn);
            }
            return n;
        }

        public ErrorTuple(JsonObject v) {
            this(v.getInteger("mode", 0), v.getInteger("code", 500), v.getString("user"), v.getString("system"));
        }

        @Override
        public DomainError make(Object... arguments) {
            if (argument == 0 || arguments.length == 0) return new DomainError(mode, code, user, system);
            var s = system != null ? MessageFormatter.arrayFormat(system, arguments) : null;
            var u = user != null ? MessageFormatter.arrayFormat(user, arguments) : null;
            Throwable e = null;
            if (s != null && s.getThrowable() != null) e = s.getThrowable();
            else if (u != null) e = u.getThrowable();
            if (e != null) {
                return new DomainError(mode, code, u == null ? null : u.getMessage(), s == null ? null : s.getMessage(),
                                       e);
            }
            return new DomainError(mode, code, u == null ? null : u.getMessage(), s == null ? null : s.getMessage());
        }

        public DomainError suppress(Object... arguments) {
            if (argument == 0 || arguments.length == 0) return new DomainError(mode, code, user, system, null, true);
            var s = system != null ? MessageFormatter.arrayFormat(system, arguments) : null;
            var u = user != null ? MessageFormatter.arrayFormat(user, arguments) : null;
            Throwable e = null;
            if (s != null && s.getThrowable() != null) e = s.getThrowable();
            else if (u != null) e = u.getThrowable();
            if (e != null) {
                return new DomainError(mode, code, u == null ? null : u.getMessage(), s == null ? null : s.getMessage(),
                                       e, true);
            }
            return new DomainError(mode, code, u == null ? null : u.getMessage(), s == null ? null : s.getMessage(),
                                   null, true);
        }
    }
}
