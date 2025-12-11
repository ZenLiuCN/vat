package vat.core;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

import java.util.Optional;
import java.util.function.Supplier;

///
/// @author Zen.Liu
/// @since 2025-11-11


public class MetricHolder  {
    public static void setMetricOptionsSupplier(Supplier<MetricsOptions> supplier) {
        MetricHolder.supplier = supplier;
    }

    private static Supplier<MetricsOptions> supplier = () -> new MicrometerMetricsOptions()
            .setEnabled(true)
            .setJvmMetricsEnabled(true)
            .setPrometheusOptions(new VertxPrometheusOptions()
                    .setEnabled(true)
                    .setPublishQuantiles(Optional.ofNullable(System.getProperty("vertx.metrics.prometheus.quantiles")).map(Boolean::parseBoolean)
                            .orElse(false))
                    .setStartEmbeddedServer(true)
                    .setEmbeddedServerOptions(new HttpServerOptions()
                            .setSsl(false)
                            .setPort(Optional.ofNullable(System.getProperty("vertx.metrics.prometheus.port")).map(Integer::parseInt)
                                    .orElse(8081)
                            )
                    )
            );


    public static void configCommands(Vertx vertx) {

    }

    public static void configOption(AbstractLauncher launcher, VertxOptions options) {
        options.setMetricsOptions(supplier.get());
    }
}
