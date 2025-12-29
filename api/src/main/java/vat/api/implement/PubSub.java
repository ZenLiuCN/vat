package vat.api.implement;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import org.jspecify.annotations.Nullable;
import vat.api.Disposable;
import vat.api.Event;

import java.util.Objects;
import java.util.function.Consumer;

///
/// @author Zen.Liu
/// @since 2025-10-27

public interface PubSub {
    interface Publish<T extends Event> extends Consumer<T> {


    }

    interface Subscribe<T extends Event> extends Consumer<T> {

    }

    static <T extends Event> Publish<T> publish(Class<T> type, @Nullable String address, Vertx vertx) {
        var codec = Codec.codec(type);
        try {
            vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
        } catch (Exception ignore) {
        }
        var addr = address == null ? type.getCanonicalName() : address;
        return e -> vertx.eventBus().publish(addr, codec.set(e));
    }

    static <T extends Event> Disposable subscribe(Class<T> type, @Nullable String address, Vertx vertx, Subscribe<T> subscriber) {
        var codec = Codec.codec(type);
        try {
            vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
        } catch (Exception ignore) {
        }
        var addr = address == null ? type.getCanonicalName() : address;
        var e = vertx.eventBus().<JsonObject>consumer(addr, ev -> subscriber.accept(Objects.requireNonNull(codec.get(ev.body()))));
        return e::unregister;
    }



}
