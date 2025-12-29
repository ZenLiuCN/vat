package vat.api.implement;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import vat.api.Activities;
import vat.api.Disposable;
import vat.api.DomainError;
import vat.api.utils.Buf;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/// @author Zen.Liu
/// @since 2025-11-02

public abstract class BaseActivities<F extends Activities, I extends BaseActivities<F, I>>
        implements Activities, DomainManager {
    protected BaseActivities(Vertx vertx, String address, Logger log) {

        this.vertx = vertx;
        this.address = address;
        this.log = log;
    }

    ///  for SPI only
    @SuppressWarnings("DataFlowIssue")
    protected BaseActivities() {
        this.vertx = null;
        this.address = null;
        this.log = null;
    }

    protected abstract I _this();

    @Getter
    @Accessors(fluent = true)
    protected final Map<String, Disposable> registry = new ConcurrentHashMap<>();
    protected final Map<String, Disposable> services = new ConcurrentHashMap<>();
    @Getter
    @Accessors(fluent = true)
    protected final Vertx vertx;
    protected final String address;
    protected final Logger log;

    /// register handler
    protected void handle(String name, Function<Optional<Buf>, Future<Optional<Buf>>> service) {
        if (services.containsKey(name)) throw new IllegalStateException(name + " already exists");
        services.put(name, vertx
                .eventBus()
                .<Buffer>consumer(address + "::" + name, m -> {
                    try {
                        service
                                .apply(Optional.of(m.body()).map(Buf::of))
                                .onComplete(ar -> {
                                    if (ar.succeeded()) {
                                        m.reply(ar.result().map(Buf::raw).orElse(null));
                                    } else {
                                        failure(log, ar.cause(), m, this.getClass());
                                    }
                                });
                    } catch (Exception e) {
                        failure(log, e, m, this.getClass());
                    }
                })::unregister);
    }

    public static void failure(Logger log, Throwable t, Message<?> msg, Class<?> owner) {
        log.error("service invoke failure: headers {} body {}", msg.headers(), msg.body(), t);
        var includeDebugInfo = log.isDebugEnabled() || log.isTraceEnabled();
        if (t instanceof ServiceException se) {
            if (includeDebugInfo) {
                msg.reply(new ServiceException(se.failureCode(), se.getMessage(), se.getDebugInfo()));
            } else {
                msg.reply(new ServiceException(se.failureCode(), se.getMessage()));
            }
        } else if (t instanceof DomainError de) {
            msg.reply(new ServiceException(de.code, de.user + "^" + de.getMessage(), de.asJson()));
        } else if (includeDebugInfo) {
            msg.reply(new ServiceException(500,
                    t.getMessage() + "|" + t.getClass().getCanonicalName(),
                    JsonObject.of("kind", t.getClass().getName(),
                            "message", t.getMessage(),
                            "owner", owner.getSimpleName(),
                            "stacktrace", DomainError.dumpStack(t))));
        } else {
            msg.reply(new ServiceException(500, t.getMessage(),
                    JsonObject.of("kind", t.getClass().getName(),
                            "message", t.getMessage(),
                            "owner", owner.getSimpleName(),
                            "stacktrace", DomainError.dumpStack(t))));
        }
    }

    public static Throwable recovery(Throwable t) {
        if (t instanceof ServiceException se) {
            var jo = se.getDebugInfo();
            if (jo != null && jo.containsKey("code")) {
                return new DomainError(jo);
            }
        }
        return t;
    }

    @Override
    public Future<Void> dispose() {
        return Future.join(Stream.concat(
                                registry.values().stream(),
                                services.values().stream()
                        )
                        .map(Disposable::dispose)
                        .toList()
        ).mapEmpty();
    }

}
