package vat.core;

import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import vat.api.Activities;
import vat.api.DomainError;
import vat.api.meta.Activity;
import vat.api.utils.Environment;
import vat.api.utils.Pointer;
import vat.core.verticles.ActivityFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Bootstrap verticle
///
/// @author Zen.Liu
/// @since 2025-11-11

public class BootstrapVerticle extends VerticleBase {
    public static final String UNDEPLOY_LISTENER = "BootstrapVerticle::undeploy";
    public static final Logger log = org.slf4j.LoggerFactory.getLogger(BootstrapVerticle.class);
    protected final String file;
    protected final String retrieverKey;
    protected MessageConsumer<String> undeployConsumer;

    protected BootstrapVerticle(String file, String retrieverKey) {
        this.file = file;
        this.retrieverKey = retrieverKey;
    }

    public BootstrapVerticle() {
        this("app.conf", "retriever");
    }

    protected  volatile ConfigRetriever local;
    protected volatile ConfigRetriever remote;
    static final Pointer disabled = Pointer.of("/disabled");
    volatile DeployManager dm;

    protected  record DeployManager(
            Vertx vertx,
            ReentrantLock lock,
            Map<String, ActivityFactory> deployed,
            Map<ActivityFactory, String> deploys,
            Set<ActivityFactory> deployable
    ) {
        DeployManager(Vertx vertx) {
            this(vertx,
                    new ReentrantLock(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new CopyOnWriteArraySet<>());
        }

        public void undeploy(String id) {
            var d = deployed.get(id);
            if (d != null) {
                log.info("{}:{}:{} undeploy", id, d.domain(), d.name());
                deployed.remove(id);
                deploys.remove(d);
            }
        }

        public void redeploy(JsonObject conf) {
            if (!deployable.isEmpty()) {
                if (!lock.tryLock()) {
                    log.warn("deploying, skip update of {}", conf);
                    return;
                }
                var redo = deployable.stream().filter(ActivityFactory::auto).toList();
                Future.join(redo.stream().map(this::undeploy).toList())
                        .flatMap($_ -> Future.join(redo.stream().map(x -> deploy(x, conf)).toList()))
                        .onComplete(r -> {
                            if (r.succeeded()) log.info("redeployed for configuration update");
                            else log.error("redeployed for configuration update", r.cause());
                            lock.unlock();
                        })
                ;
            }
        }

        protected  Future<Void> deploy(ActivityFactory x, JsonObject newc) {
            return vertx.deployVerticle(x.make(newc))
                    .onSuccess(id -> {
                        deployed.put(id, x);
                        deploys.put(x, id);
                        log.info("{}:{} deployed as {}", x.domain(), x.name(), id);
                    })
                    .mapEmpty();
        }

        protected Future<Void> undeploy(ActivityFactory f) {
            var id = deploys.get(f);
            if (id == null) return Future.succeededFuture();
            return vertx.undeploy(id);
        }

        public Future<Void> dispose() {
            if (deployed.isEmpty()) return Future.succeededFuture();
            return Future.join(deployed.keySet()
                            .stream()
                            .map(x -> vertx.undeploy(x)
                                    .recover(ex -> {
                                        log.error("undeploy {}", x, ex);
                                        return Future.succeededFuture();
                                    }))
                            .toList())
                    .mapEmpty();
        }
    }

    protected void disposedListener(Message<String> m) {
        var id = m.body();
        dm.undeploy(id);
    }

    protected void localRetriever(Promise<ConfigRetriever> p) {
        local = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .setScanPeriod(60_000L)
                .addStore(
                        Environment.string("vat.config")
                                .map(JsonObject::new)
                                .map(ConfigStoreOptions::new)
                                .orElseGet(() ->
                                        new ConfigStoreOptions()
                                                .setType("file")
                                                .setFormat("hocon")
                                                .setConfig(JsonObject.of("path", this.file)))))
        ;
        local.listen(this::onLocalChange);
        p.complete(local);
    }

    protected Future<ApplicationConfiguration.Data> remoteRetriever(ApplicationConfiguration.Data conf) {
        return conf.retriever()
                .map(x -> {
                    remote = ConfigRetriever.create(vertx, new ConfigRetrieverOptions(x));
                    remote.listen(this::onRemoteChange);
                    return remote.getConfig().map(ApplicationConfiguration.Data::new);
                })
                .orElse(Future.succeededFuture(conf));
    }

    private void onRemoteChange(ConfigChange configChange) {
        var newc = configChange.getNewConfiguration();
        if (newc != null) {
            dm.redeploy(newc);
        }
    }

    private void onLocalChange(ConfigChange configChange) {
        if (remote != null) {
            log.warn("ignore local configuration change for remote retriever enabled");
            return;
        }
        var newc = configChange.getNewConfiguration();
        if (newc != null) {
            dm.redeploy(newc);
        }
    }


    static final Consumer<?> NOOP = c -> {
    };

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> noop() {
        return (Consumer<T>) NOOP;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected <T> Class<T> load(String name) {
        return (Class<T>) Class.forName(name);
    }

    protected Future<Void> loadVerticle(ApplicationConfiguration.Data conf) {
        dm = new DeployManager(vertx);
        var disabled = conf.disabled().orElse(List.of())
                .stream()
                .map(x -> x instanceof String s ? s : null)
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());
        var manually = conf.manual().orElse(List.of())
                .stream()
                .map(x -> x instanceof String s ? s : null)
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .<Class<? extends Activities>>map(this::load)
                .collect(Collectors.toSet());
        var candidates = Stream.concat(
                        conf.discovery().orElse(true)
                                ? ServiceLoader
                                .load(Activities.class, Thread.currentThread().getContextClassLoader())
                                .stream().map(ServiceLoader.Provider::type)
                                : Stream.empty()
                        ,
                        manually.stream()
                )
                .filter(x -> x.getDeclaredAnnotation(Activity.class) != null && !disabled.contains(x.getCanonicalName()))
                .peek(log.isDebugEnabled() ? (c -> log.debug("found activities {}", c)) : noop())
                .map(BootstrapVerticle::buildActivity)
                .distinct()
                .sorted(Comparator.comparingInt(ActivityFactory::mode))
                .toList();
        if (candidates.isEmpty()) throw DomainError.System.internalServerError("missing activity to deploy");
        dm.deployable.addAll(candidates);
        var config = conf.asJson();
        return Future.join(candidates.stream()
                        .map(x -> dm.deploy(x, config))
                        .toList())
                .mapEmpty();
    }

    static ActivityFactory buildActivity(Class<? extends Activities> proto) {
        var a = proto.getDeclaredAnnotation(Activity.class);
        var mode = a.order() != -1 ? a.order() : a.mode().ordinal();
        var auto = a.auto();
        return ActivityFactory.decide(mode, auto, proto);
    }

    @Override
    public Future<?> start() {
        undeployConsumer = vertx.eventBus().localConsumer(UNDEPLOY_LISTENER, this::disposedListener);
        return Future.future(this::localRetriever)
                .flatMap(ConfigRetriever::getConfig)
                .map(ApplicationConfiguration.Data::new)
                .flatMap(this::remoteRetriever)
                .flatMap(this::loadVerticle)
                .mapEmpty();
    }

    @Override
    public Future<?> stop() {
        return Future.join(List.of(
                        local.close(),
                        remote == null ? Future.succeededFuture() : remote.close(),
                        undeployConsumer == null ? Future.succeededFuture() : undeployConsumer.unregister()
                ))
                .flatMap($ -> dm.dispose());
    }
}
