package vat.api.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.jspecify.annotations.NullMarked;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(value = 2, jvmArgs = {"-Xms512M", "-Xmx512M", "-XX:+UseParallelGC"})
@NullMarked
public class MonadicArchitectureBenchmark {

    private Vertx vertx;
    private Monadic<String, Integer, Integer> listPipeline;
    private Monadic<String, Integer, Integer> arrayPipeline;
    private Monadic<String, Integer, Integer> reusedArrayPipeline;

    private static final Function<Integer, Integer> MAP_OP = v -> v + 17; // Using constant for CTX.length()
    private static final Function<Integer, Future<Integer>> FLAT_MAP_OP =
            v -> (v % 10 == 0) ? Future.succeededFuture(v * 2) : Future.succeededFuture(v);
    private static final Function<Throwable, Future<Integer>> RECOVER_OP =
            err -> Future.succeededFuture(0);

    private static final BiFunction<String, Integer, Integer> MONADIC_MAP =
            (ctx, v) -> v + ctx.length();

    private static final Predicate<Integer> FLAT_MAP_CONDITION =
            v -> v % 10 == 0;

    private static final Function<Integer, Future<Integer>> FLAT_MAP_ACTION =
            v -> Future.succeededFuture(v * 2);

    private static final Function<Throwable, Future<Integer>> RECOVER_FUNC =
            err -> Future.succeededFuture(0); // Note: stepIdx can't be static, using 0 for baseline
    private static final int CHAIN_LENGTH = 1000;
    private static final String CTX = "benchmark-context";

    @Setup
    public void setup() {
        vertx = Vertx.vertx();

        // 1. Build Monadic Pipeline
        Monadic<String, Integer, Integer> p = Monadic.identity();
        Monadic<String, Integer, Integer> p2 = Monadic.identity();
        for (int i = 0; i < CHAIN_LENGTH; i++) {
            final int stepIdx = i;
            p = p.map((ctx, v) -> v + ctx.length())
                    .flatMapIf(v -> v % 10 == 0, v -> Future.succeededFuture(v * 2))
                    .recover(err -> Future.succeededFuture(stepIdx));
            // Reused Path: points to the exact same static method references
            p2 = p2.map(MONADIC_MAP)
                    .flatMapIf(FLAT_MAP_CONDITION, FLAT_MAP_ACTION)
                    .recover(RECOVER_FUNC);
        }
        this.listPipeline = p;
        this.arrayPipeline = p.finalization();
        this.reusedArrayPipeline = p2.finalization();


    }
    @Benchmark
    public Object benchmarkStandardSetup() {
        Monadic<String, Integer, Integer> p = Monadic.identity();
        for (int i = 0; i < 1000; i++) {
            final int stepIdx = i;
            p = p.map((ctx, v) -> v + ctx.length())
                    .flatMapIf(v -> v % 10 == 0, v -> Future.succeededFuture(v * 2))
                    .recover(err -> Future.succeededFuture(stepIdx));
        }
        return p.finalization(); // Trigger the array conversion
    }

    @Benchmark
    public Object benchmarkReusedSetup() {
        Monadic<String, Integer, Integer> p = Monadic.identity();
        for (int i = 0; i < 1000; i++) {
            p = p.map(MONADIC_MAP)
                    .flatMapIf(FLAT_MAP_CONDITION, FLAT_MAP_ACTION)
                    .recover(RECOVER_FUNC);
        }
        return p.finalization();
    }
    @Benchmark
    public void benchmarkRawVertx(Blackhole bh) {
        Future<Integer> pipeline = Future.succeededFuture(0);

        for (int i = 0; i < CHAIN_LENGTH; i++) {
            final int stepIdx = i;
            pipeline = pipeline
                    .map(v -> v + CTX.length())
                    .flatMap(v -> (v % 10 == 0) ? Future.succeededFuture(v * 2) : Future.succeededFuture(v))
                    .recover(err -> Future.succeededFuture(stepIdx));
        }

        bh.consume(waitFor(pipeline));
    }
    @Benchmark
    public void benchmarkRawVertxOptimized(Blackhole bh) {
        Future<Integer> pipeline = Future.succeededFuture(0);

        for (int i = 0; i < CHAIN_LENGTH; i++) {
            // Even with reused lambdas, we still call .map/.flatMap 1000 times
            pipeline = pipeline
                    .map(MAP_OP)
                    .flatMap(FLAT_MAP_OP)
                    .recover(RECOVER_OP);
        }

        bh.consume(waitFor(pipeline));
    }
    @Benchmark
    public void benchmarkListSteps(Blackhole bh) {
        // Monadic List Implementation
        bh.consume(waitFor(listPipeline.process(CTX, 0)));
    }

    @Benchmark
    public void benchmarkArrayFinalized(Blackhole bh) {
        // Monadic Array (Optimized) Implementation
        bh.consume(waitFor(arrayPipeline.process(CTX, 0)));
    }
    @Benchmark
    public void benchmarkArrayReuseFinalized(Blackhole bh) {
        // Monadic Array (Optimized) Implementation
        bh.consume(waitFor(reusedArrayPipeline.process(CTX, 0)));
    }
    private <T> T waitFor(Future<T> future) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        future.onComplete(ar -> {
            if (ar.succeeded()) cf.complete(ar.result());
            else cf.completeExceptionally(ar.cause());
        });
        return cf.join();
    }

    @TearDown
    public void tearDown() {
        vertx.close();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(MonadicArchitectureBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class) // This captures the memory metrics
                .jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
                .build();
        new Runner(opt).run();
    }
}
