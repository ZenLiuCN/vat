package vat.core;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import vat.api.utils.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

///
/// @author Zen.Liu
/// @since 2025-11-11

@Slf4j
public abstract class AbstractLauncher implements VertxApplicationHooks {
    protected AbstractLauncher main;
    protected String name;
    protected abstract AbstractLauncher self();
    protected final Map<String, BiConsumer<Vertx, Logger>> shutdownHooks = new HashMap<>();
    protected final Map<String, BiFunction<Vertx, Logger, Future<Void>>> closeHooks = new HashMap<>();

    protected AbstractLauncher() {
        main = self();
        this.name = Environment.string("vat.service")
                .or(() -> Optional.ofNullable(name).filter(Predicate.not(String::isBlank)))
                .or(() -> Optional.of(this.getClass().getSimpleName()))
                .get()
        ;
    }

    @Override
    public void afterVertxStarted(HookContext ctx) {
        var vertx = ctx.vertx();
        if (!closeHooks.isEmpty() && vertx instanceof VertxInternal vi) {
            var cp = new HashMap<>(closeHooks);
            log.info("register stop hooks: {}", cp);
            vi.addCloseHook(p -> {
                var f = new ArrayList<Future<Void>>();
                cp.forEach((k, v) -> f.add(v.apply(vi, log)));
                Future.all(f).<Void>mapEmpty().onComplete(p);
            });
        }
        if (!shutdownHooks.isEmpty()) {
            var cp = new HashMap<>(shutdownHooks);
            log.info("register shutdown hooks: {}", shutdownHooks);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> cp.forEach((k, v) -> v.accept(vertx, log))));
        }
        try {
            Class.forName("io.vertx.ext.shell.command.CommandRegistry");
            HAVE_COMMAND = true;
        } catch (Exception ex) {
            HAVE_COMMAND = false;
        }
        if (HAVE_COMMAND) configureCommands(vertx);
    }
    public static boolean HAVE_COMMAND=false;
    protected boolean enabledMetric() {
        return "true".equalsIgnoreCase(System.getProperty("vertx.metrics.enabled"));
    }

    protected boolean enableTracing() {
        return "true".equalsIgnoreCase(System.getProperty("vertx.tracing.enabled"));
    }

    @SneakyThrows
    protected void configureCommands(Vertx vertx) {
        if (enabledMetric()) {
            vat.core.MetricHolder.configCommands(vertx);
        }
        if (enableTracing()) {
            vat.core.TracingHolder.configCommands(vertx);
        }
    }

    @SneakyThrows
    @Override
    public void beforeStartingVertx(HookContext ctx) {
        var options = ctx.vertxOptions();
        if (enabledMetric()) {
            vat.core.MetricHolder.configOption(this, options);
        }
        if (enableTracing()) {
            vat.core.TracingHolder.configOption(this, options);
        }
        options.setPreferNativeTransport(true);
    }

    public static void main(String[] args) {
        var vertxApplication = new VertxApplication(
                args.length == 0 ? new String[]{BootstrapVerticle.class.getCanonicalName()} : args,
                new AbstractLauncher() {
                    @Override
                    protected AbstractLauncher self() {
                        return this;
                    }
                });
        vertxApplication.launch();

    }

    public static void launch(String[] args, Supplier<? extends AbstractLauncher> launcher) {
        var vertxApplication = new VertxApplication(
                args.length == 0 ? new String[]{BootstrapVerticle.class.getCanonicalName()} : args,
                launcher.get());
        vertxApplication.launch();
    }

}
