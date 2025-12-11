package vat.api.implement;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import vat.api.Activities;
import vat.api.utils.Buf;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static vat.api.implement.BaseActivities.recovery;

/// @author Zen.Liu
/// @since 2025-11-02

public abstract class BaseActivitiesProxy implements Activities {
    public static final AtomicReference<DeliveryOptions> OPTIONS=new AtomicReference<>(new DeliveryOptions());
    @Getter
    @Accessors(fluent = true)
    protected final Vertx vertx;
    @Getter
    @Accessors(fluent = true)
    protected final String address;
    @Getter
    @Accessors(fluent = true)
    protected final DeliveryOptions options;

    protected BaseActivitiesProxy(Vertx vertx, String address, @Nullable DeliveryOptions options) {
        this.vertx = vertx;
        this.address = address;
        this.options = options == null ? new DeliveryOptions(OPTIONS.get()) : options;
        try {
            vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
        } catch (Exception ignore) {
        }
    }


    protected Future<Optional<Buf>> invoke(String name, @Nullable Buf argument) {
        return vertx.eventBus()
                .<Buffer>request(address + "::" + name, argument == null ? null : argument.raw())
                .map(Message::body)
                .map(Optional::ofNullable)
                .map(b -> b.map(Buf::of))
                .recover(ex -> Future.failedFuture(recovery(ex)));
    }

    @Override
    public Future<Void> dispose() {
        return Future.succeededFuture();
    }
}
