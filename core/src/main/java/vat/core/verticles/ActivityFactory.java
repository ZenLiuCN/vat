package vat.core.verticles;

import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import vat.api.Activities;
import vat.api.DomainError;
import vat.api.implement.DomainManager;
import vat.api.implement.Web;
import vat.api.store.Dialect;

import java.lang.reflect.Constructor;

///
/// @author Zen.Liu
/// @since 2025-11-11
public interface ActivityFactory {
    int mode();

    boolean auto();

    String name();

    /// domain simple name used as configuration key
    String domain();

    VerticleBase make(JsonObject conf);

    static Constructor<? extends Activities> find(Class<? extends Activities> type, Class<?>... parameters) {
        try {
            return type.getConstructor(parameters);
        } catch (NoSuchMethodException ignore) {
            return null;
        }
    }

    @SneakyThrows
    static ActivityFactory decide(int mode, boolean auto, Class<? extends Activities> t) {
        var ctor = find(t, Vertx.class,String.class);
        if (ctor != null) return new F1.Impl(mode, auto, t.getCanonicalName(), DomainManager.domain(t), ctor::newInstance);
        ctor = find(t, Vertx.class,String.class, JsonObject.class);
        if (ctor != null) return new F2.Impl(mode, auto, t.getCanonicalName(), DomainManager.domain(t), ctor::newInstance);
        ctor = find(t, Vertx.class,String.class, Web.Factory.class, JsonObject.class);
        if (ctor != null) return new F3.Impl(mode, auto, t.getCanonicalName(),  DomainManager.domain(t),ctor::newInstance);
        ctor = find(t, Vertx.class,String.class, Pool.class, Dialect.class, JsonObject.class);
        if (ctor != null) return new F4.Impl(mode, auto, t.getCanonicalName(), DomainManager.domain(t), ctor::newInstance);
        ctor = find(t, Vertx.class,String.class,Web.Factory.class, Pool.class, Dialect.class,  JsonObject.class);
        if (ctor != null) return new F5.Impl(mode, auto, t.getCanonicalName(), DomainManager.domain(t), ctor::newInstance);
        throw DomainError.System.internalServerError("invalid {} constructor signature", t);
    }





}
