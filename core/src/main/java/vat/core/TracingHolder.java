package vat.core;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.incubating.ContainerIncubatingAttributes;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import org.jooq.lambda.Sneaky;
import vat.api.implement.BaseActivitiesProxy;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-11-11


public class TracingHolder  {


    public static void setConfigurator(Runnable config) {
        if (beforeConfig != null) {
            beforeConfig = () -> {
                beforeConfig.run();
                config.run();
            };
        } else {
            beforeConfig = config;
        }

    }

    static void containerAttributes(AttributesBuilder attributes) {
        Optional<String> id;
        {
            var v1CgroupPath = Paths.get("/proc/self/cgroup");
            var v2CgroupPath = Paths.get("/proc/self/mountinfo");
            var readable = (Predicate<Path>) Files::isReadable;
            Function<Path, Stream<String>> lines = Sneaky.function(Files::lines);
            Function<String, Optional<String>> idFromLine = line -> {
                // This cgroup output line should have the container id in it
                int lastSlashIdx = line.lastIndexOf('/');
                if (lastSlashIdx < 0) {
                    return Optional.empty();
                }

                String containerId;

                String lastSection = line.substring(lastSlashIdx + 1);
                int colonIdx = lastSection.lastIndexOf(':');

                if (colonIdx != -1) {
                    // since containerd v1.5.0+, containerId is divided by the last colon when the cgroupDriver is
                    // systemd:
                    // https://github.com/containerd/containerd/blob/release/1.5/pkg/cri/server/helpers_linux.go#L64
                    containerId = lastSection.substring(colonIdx + 1);
                } else {
                    int startIdx = lastSection.lastIndexOf('-');
                    int endIdx = lastSection.lastIndexOf('.');

                    startIdx = startIdx == -1 ? 0 : startIdx + 1;
                    if (endIdx == -1) {
                        endIdx = lastSection.length();
                    }
                    if (startIdx > endIdx) {
                        return Optional.empty();
                    }

                    containerId = lastSection.substring(startIdx, endIdx);
                }
                if (OtelEncodingUtils.isValidBase16String(containerId) && (containerId.length() == 64)) {
                    return Optional.of(containerId);
                } else {
                    return Optional.empty();
                }
            };

            if (readable.test(v1CgroupPath)) {
                try (var ls = lines.apply(v1CgroupPath)) {
                    id = ls
                            .filter(line -> !line.isEmpty())
                            .map(idFromLine)
                            .filter(Optional::isPresent)
                            .findFirst()
                            .orElse(Optional.empty());
                } catch (Exception ignore) {
                    id = Optional.empty();
                }
            } else if (readable.test(v2CgroupPath)) {
                var CONTAINER_ID_RE = Pattern.compile("^[0-9a-f]{64}$");
                var CRI_CONTAINER_ID_RE = Pattern.compile("cri-containerd:[0-9a-f]{64}");
                try (var ls = lines.apply(v2CgroupPath)) {
                    var line = ls.toList();
                    id = line.stream()
                            .filter(l -> l.contains("/containers/"))
                            .flatMap(l -> Stream.of(l.split("/")))
                            .map(CONTAINER_ID_RE::matcher)
                            .filter(Matcher::matches)
                            .reduce((first, second) -> second)
                            .map(matcher -> matcher.group(0));
                    if (id.isEmpty()) {
                        id = line.stream()
                                .filter(l -> l.contains("cri-containerd:"))
                                .map(CRI_CONTAINER_ID_RE::matcher)
                                .filter(Matcher::find)
                                .findFirst()
                                .map(matcher -> matcher.group(0).substring(15));
                    }
                } catch (Exception ignore) {
                    id = Optional.empty();
                }
            } else {
                id = Optional.empty();
            }
        }
        id.ifPresent(v -> attributes.put(ContainerIncubatingAttributes.CONTAINER_ID, v));
    }

    static void hostAttributes(AttributesBuilder attributes) {
        try {
            attributes.put(HostIncubatingAttributes.HOST_NAME, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ignore) {
        }
        try {
            attributes.put(HostIncubatingAttributes.HOST_ARCH, System.getProperty("os.arch"));
        } catch (SecurityException ignore) {
        }

    }

    static void jvmAttributes(AttributesBuilder attributes) {
        try {
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_NAME, System.getProperty("java.runtime.name"));
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_VERSION,
                    System.getProperty("java.runtime.version"));
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_DESCRIPTION,
                    System.getProperty("java.vm.vendor") +
                    " " + System.getProperty("java.vm.name") +
                    " " + System.getProperty("java.vm.version"));
        } catch (Exception ignore) {
        }

    }

    private static String getOs(String os) {
        os = os.toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.WINDOWS;
        } else if (os.startsWith("linux")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.LINUX;
        } else if (os.startsWith("mac")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.DARWIN;
        } else if (os.startsWith("freebsd")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.FREEBSD;
        } else if (os.startsWith("netbsd")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.NETBSD;
        } else if (os.startsWith("openbsd")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.OPENBSD;
        } else if (os.startsWith("dragonflybsd")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.DRAGONFLYBSD;
        } else if (os.startsWith("hp-ux")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.HPUX;
        } else if (os.startsWith("aix")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.AIX;
        } else if (os.startsWith("solaris")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.SOLARIS;
        } else if (os.startsWith("z/os")) {
            return OsIncubatingAttributes.OsTypeIncubatingValues.Z_OS;
        }
        return null;
    }

    static void osAttributes(AttributesBuilder attributes) {
        try {
            String os;
            try {
                os = System.getProperty("os.name");
            } catch (SecurityException t) {
                // Security manager enabled, can't provide much os information.
                return;
            }
            if (os == null) {
                return;
            }

            var osName = getOs(os);
            if (osName != null) {
                attributes.put(OsIncubatingAttributes.OS_TYPE, osName);
            }

            String version = null;
            try {
                version = System.getProperty("os.version");
            } catch (SecurityException e) {
                // Ignore
            }
            String osDescription = version != null ? os + ' ' + version : os;
            attributes.put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription);
        } catch (Exception ignore) {
        }


    }

    static final Pattern JAR_FILE_PATTERN =
            Pattern.compile("^\\S+\\.(jar|war)", Pattern.CASE_INSENSITIVE);

    static void processAttributes(AttributesBuilder attributes) {
        var runtime = ManagementFactory.getRuntimeMXBean();

        var pid = -1L;
        var runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        var atIndex = runtimeName.indexOf('@');
        if (atIndex >= 0) {
            String pidString = runtimeName.substring(0, atIndex);
            try {
                pid = Long.parseLong(pidString);
            } catch (NumberFormatException ignored) {
                // Ignore parse failure.
            }
        }
        if (pid >= 0) {
            attributes.put(ProcessIncubatingAttributes.PROCESS_PID, pid);
        }

        String javaHome = null;
        String osName = null;
        try {
            javaHome = System.getProperty("java.home");
            osName = System.getProperty("os.name");
        } catch (SecurityException e) {
            // Ignore
        }
        if (javaHome != null) {
            StringBuilder executablePath = new StringBuilder(javaHome);
            executablePath
                    .append(File.separatorChar)
                    .append("bin")
                    .append(File.separatorChar)
                    .append("java");
            if (osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows")) {
                executablePath.append(".exe");
            }

            attributes.put(ProcessIncubatingAttributes.PROCESS_EXECUTABLE_PATH, executablePath.toString());

            String[] args;
            StringBuilder commandLine = new StringBuilder(executablePath);
            for (String arg : runtime.getInputArguments()) {
                commandLine.append(' ').append(arg);
            }
            // sun.java.command isn't well document and may not be available on all systems.
            var javaCommand = System.getProperty("sun.java.command");
            if (javaCommand != null) {
                // This property doesn't include -jar when launching a jar directly.  Try to determine
                // if that's the case and add it back in.
                if (JAR_FILE_PATTERN.matcher(javaCommand).matches()) {
                    commandLine.append(" -jar");
                }
                commandLine.append(' ').append(javaCommand);
            }
            attributes.put(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE, commandLine.toString());

        }

    }

    static void processRuntimeAttributes(AttributesBuilder attributes) {
        try {
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_NAME, System.getProperty("java.runtime.name"));
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_VERSION,
                    System.getProperty("java.runtime.version"));
            attributes.put(ProcessIncubatingAttributes.PROCESS_RUNTIME_DESCRIPTION,
                    System.getProperty("java.vm.vendor")
                    + " "
                    + System.getProperty("java.vm.name")
                    + " "
                    + System.getProperty("java.vm.version"));
        } catch (SecurityException ignored) {

        }

    }

    private static Runnable beforeConfig;



    public static void configCommands(Vertx vertx) {

    }

    public static SdkTracerProvider sdkTracerProvider;
    public static OpenTelemetrySdk openTelemetry;


    public  static void configOption(AbstractLauncher launcher, VertxOptions options) {
        System.setProperty("io.opentelemetry.context.contextStorageProvider",
                "io.vertx.tracing.opentelemetry.VertxContextStorageProvider");
        var compress = Optional.ofNullable(System.getProperty("vertx.tracing.telemetry.compress")).orElse("none");
        var endpoint = Optional.ofNullable(System.getProperty("vertx.tracing.telemetry.endpoint")).orElseThrow();
        var attr = Attributes.builder().put(ServiceAttributes.SERVICE_NAME, launcher.name);
        containerAttributes(attr);
        osAttributes(attr);
        hostAttributes(attr);
        jvmAttributes(attr);
        processAttributes(attr);
        processRuntimeAttributes(attr);
        var res = Resource.create(attr.build());
        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
                                .setCompression(compress)
                                .setEndpoint(endpoint)
                                .build())
                        .build())
                .setResource(res)
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
        if (beforeConfig != null) beforeConfig.run();
        if (Optional.ofNullable(System.getProperty("vertx.tracing.system.enabled")).map(Boolean::parseBoolean).orElse(false)) {
            var op = BaseActivitiesProxy.OPTIONS.get();
            op = op == null ? new DeliveryOptions() : new DeliveryOptions(op);
            op.setTracingPolicy(TracingPolicy.ALWAYS);
            BaseActivitiesProxy.OPTIONS.set(op);
        }
        options.setTracingOptions(new OpenTelemetryOptions(openTelemetry));
    }
}
