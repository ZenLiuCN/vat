package vat.api.utils;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vat.api.Data;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

///
/// @author Zen.Liu
/// @since 2025-11-26


public interface Executors {
    Logger log = LoggerFactory.getLogger(Executors.class);

    /**
     *
     *
     * @param vertx vertx
     * @see #ofVertxWorkers(Vertx)
     */
    static Executor ofVertxContext(Vertx vertx) {
        return new VertxContextExecutor(vertx);
    }

    /**
     *
     * @param vertx vertx
     * @see #ofVertxContext(Vertx)
     */
    static Executor ofVertxWorkers(Vertx vertx) {
        return new VertxWorkerExecutor(vertx);
    }

    record VertxContextExecutor(
            Vertx vertx
    ) implements Executor {
        @Override
        public void execute(@NotNull Runnable runnable) {
            vertx.runOnContext(v -> runnable.run());
        }
    }

    record VertxWorkerExecutor(
            Vertx vertx
    ) implements Executor {
        @Override
        public void execute(@NotNull Runnable runnable) {
            if (vertx.getOrCreateContext().isWorkerContext()) {
                vertx.runOnContext($ -> runnable.run());
            } else {
                vertx.executeBlocking(() -> {
                    runnable.run();
                    return null;
                }).onFailure(err -> log.error("Executor on vertx worker pool fail: {}", runnable, err));
            }

        }
    }

    record ExecutorDefine(
            JsonObject json
    ) implements Data {
        public boolean isEmpty() {
            return json.isEmpty();
        }

        public String getName(String def) {
            return json.getString("name", def);
        }

        public int getCore(int core) {
            return Optional.ofNullable(json.getFloat("core"))
                    .map(x -> x < 0 ? Runtime.getRuntime().availableProcessors() * (-x) : x == 0 ? core : x)
                    .map(x -> (int) (float) x)
                    .orElse(core);
        }

        public int getCap(int cap) {
            return Optional.ofNullable(json.getFloat("cap"))
                    .map(x -> x < 0 ? Runtime.getRuntime().availableProcessors() * (-x) : x == 0 ? cap : x)
                    .map(x -> (int) (float) x)
                    .orElse(cap);
        }

        public long getTTL(long def) {
            return Optional.ofNullable(json.getLong("ttl")).orElse(def);
        }
        public boolean getDaemon() {
            return json.getBoolean("daemon", false);
        }

        public ExecutorDefine name(String name) {
            json.put("name", name);
            return this;
        }

        public ExecutorDefine daemon(boolean daemon) {
            json.put("daemon", daemon);
            return this;
        }

        public ExecutorDefine core(float core) {
            json.put("core", core);
            return this;
        }

        public ExecutorDefine cap(float cap) {
            json.put("cap", cap);
            return this;
        }

        public ExecutorDefine ttl(Long ttl) {
            json.put("ttl", ttl);
            return this;
        }

        @Override
        public JsonObject asJson() {
            return json;
        }
    }

    static ExecutorService executorOf(@Nullable ExecutorDefine conf, String defName, int core, int cap, long ttlMills, @Nullable ExecutorService def) {
        if ((conf == null || conf.isEmpty()) && def != null) return def;
        if (conf == null) throw new IllegalStateException("config is empty");
        var name =    conf.getName(defName);;
        var daemon =  conf.getDaemon();
        return new ThreadPoolExecutor(
                conf.getCore(core),
                conf.getCap(cap),
                conf.getTTL(ttlMills),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    final AtomicInteger count = new AtomicInteger();

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        var t = new Thread(null, r, name + "_" + count.incrementAndGet());
                        try {
                            if (t.isDaemon() != daemon) t.setDaemon(daemon);
                            if (t.getPriority() != 5) t.setPriority(5);
                        } catch (Exception ignore) {
                        }
                        return t;
                    }
                });
    }

    static ExecutorService ftlExecutorOf(@Nullable ExecutorDefine conf, String defName, int core, int cap, long ttlMills, @Nullable ExecutorService def) {
        if ((conf == null || conf.isEmpty()) && def != null) return def;
        if (conf == null) throw new IllegalStateException("config is empty");
        var name = conf.getName(defName);
        return new ThreadPoolExecutor(
                conf.getCore(core),
                conf.getCap(cap),
                conf.getTTL(ttlMills),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory(name, conf.getDaemon()));
    }
}
