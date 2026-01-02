package vat.api.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@NullMarked
class MonadicUnifiedTest {

    private static final String CTX = "TestContext";

    // --- 1. CORE & PIPELINE OPTIMIZATION ---

    @Test
    @Order(1)
    @DisplayName("Test Finalization and Optimized Execution Path")
    void testFinalization(VertxTestContext vtc) {
        Monadic<String, Integer, Integer> pipeline = Monadic.<String, Integer>identity()
                .map(i -> i + 1)
                .mapCtx((c, i) -> i * 2)
                .finalization(); // Converts lazy Steps to Finalized (array-based)

        assertTrue(pipeline instanceof Monadic.Finalized);

        pipeline.process(CTX, 5).onComplete(vtc.succeeding(res -> {
            vtc.verify(() -> assertEquals(12, res));
            vtc.completeNow();
        }));
    }

    @Test
    @Order(2)
    @DisplayName("Test Optional Mapping (mapOpt) and Peek with Conditions")
    void testMapOptAndPeek(VertxTestContext vtc) {
        AtomicBoolean sideEffect = new AtomicBoolean(false);
        Monadic.<String, String>identity()
                .peekCtx((c, v) -> v.equals("hi"), (c, v) -> sideEffect.set(true))
                .mapOpt(s -> s.length() > 3 ? s : null)
                .process(CTX, "hi")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> {
                        assertEquals(Optional.empty(), res);
                        assertTrue(sideEffect.get());
                    });
                    vtc.completeNow();
                }));
    }

    // --- 2. ADVANCED BATCH OPERATIONS ---

    @Test
    @Order(3)
    @DisplayName("Batch: Parallel Filtering, Grouping, and Reduction")
    void testBatchAdvanced(VertxTestContext vtc) {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6);

        Monadic.<String, List<Integer>>identity()
                .asBatch(l -> l)
                .filterParCtx((c, i) -> Future.succeededFuture(i % 2 == 0))
                .groupCtx((c, i) -> i > 3 ? "Large" : "Small", (c, i) -> i * 10)
                .process(CTX, data)
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> {
                        assertEquals(2, res.size());
                        assertEquals(List.of(20), res.get("Small"));
                        assertEquals(List.of(40, 60), res.get("Large"));
                    });
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(4)
    @DisplayName("Batch: Concurrency Limited Parallel Map (Fast Path vs Async Path)")
    void testBatchConcurrency(Vertx vertx, VertxTestContext vtc) {
        List<Integer> data = List.of(1, 2, 3, 4);

        Monadic.<String, List<Integer>>identity()
                .asBatch(l -> l)
                .mapEachParCtx(2, (c, i) -> {
                    if (i % 2 == 0) return Future.succeededFuture(i * 10); // Fast path
                    return Future.future(p -> vertx.setTimer(10, id -> p.complete(i * 10))); // Async path
                })
                .process(CTX, data)
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals(List.of(10, 20, 30, 40), res));
                    vtc.completeNow();
                }));
    }

    // --- 3. RESOURCE MANAGEMENT & ERROR RECOVERY ---

    @Test
    @Order(5)
    @DisplayName("Bracket: Resource acquisition, use, and release")
    void testBracket(VertxTestContext vtc) {
        AtomicBoolean released = new AtomicBoolean(false);

        Monadic.<String, String>identity()
                .bracketCtx(
                        (c, res) -> Future.succeededFuture("Used " + res + " in " + c),
                        (c, res) -> {
                            released.set(true);
                            return Future.succeededFuture();
                        }
                )
                .process(CTX, "Resource")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> {
                        assertEquals("Used Resource in TestContext", res);
                        assertTrue(released.get());
                    });
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(6)
    @DisplayName("Recover: Conditional and Contextual Recovery")
    void testComplexRecovery(VertxTestContext vtc) {
        Monadic.<String, String, String>from((c, i) -> Future.failedFuture(new RuntimeException("Logic Error")))
                .recoverCtx(RuntimeException.class, (ctx, err) ->
                        err.getMessage().contains("Logic")
                                ? Future.succeededFuture("Fixed in " + ctx)
                                : Future.failedFuture(err))
                .process("Admin", "go")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals("Fixed in Admin", res));
                    vtc.completeNow();
                }));
    }

    // --- 4. RESILIENCE & BRANCHING ---

    @Test
    @Order(7)
    @DisplayName("Race and RaceWith: Multiple competitors")
    void testRaceCtx(Vertx vertx, VertxTestContext vtc) {
        Monadic<String, String, String> p1 = (c, i) -> Future.future(p -> vertx.setTimer(50, id -> p.complete("Slow")));
        Monadic<String, String, String> p2 = (c, i) -> Future.succeededFuture("Fast");

        p1.raceWith(p2)
                .process(CTX, "start")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals("Fast", res));
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(8)
    @DisplayName("FlatMapIf and Check: Conditional Execution")
    void testConditionalFlows(VertxTestContext vtc) {
        Monadic.<String, Integer>identity()
                .flatMapIfCtx((c, i) -> i < 10, (c, i) -> Future.succeededFuture(i + 100))
                .check(i -> i > 100, i -> Future.succeededFuture())
                .process(CTX, 5)
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals(105, res));
                    vtc.completeNow();
                }));
    }

    // --- 5. EDGE CASES & INTERNAL UTILS ---

    @Test
    @Order(9)
    @DisplayName("Error: Null Future Guard in Step Processing")
    void testNullFutureHandling(VertxTestContext vtc) {
        // Construct a Steps instance with a step that violates the contract by returning null
        List<Monadic.Steps.Step<String, Object, Object>> steps = new ArrayList<>();
        steps.add((c, ar) -> null);

        new Monadic.Steps<String, Object, Object>(steps)
                .process(CTX, "in")
                .onComplete(vtc.failing(err -> {
                    vtc.verify(() -> assertTrue(err instanceof NullPointerException));
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(10)
    @DisplayName("Context: withContext Back Mapping")
    void testContextMapping(VertxTestContext vtc) {
        Monadic<Integer, String, String> original = (ctx, in) -> Future.succeededFuture(in + ctx);

        original.withContext((String s) -> Integer.parseInt(s))
                .process("123", "Val-")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals("Val-123", res));
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(11)
    @DisplayName("Resilience: Circuit Breaker with Context")
    void testCircuitBreakerCtx(Vertx vertx, VertxTestContext vtc) {
        CircuitBreaker cb = CircuitBreaker.create("test", vertx, new CircuitBreakerOptions().setMaxFailures(1));

        Monadic.<String, String>identity()
                .withBreakerCtx(c -> cb, (c, v) -> Future.succeededFuture(v + c))
                .process("!", "Hello")
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals("Hello!", res));
                    vtc.completeNow();
                }));
    }

    @Test
    @Order(12)
    @DisplayName("Internal: RetryPolicy and ZipPar")
    void testZipAndRetry(Vertx vertx, VertxTestContext vtc) {
        Monadic<String, Integer, Integer> m1 = (c, i) -> Future.succeededFuture(i + 1);
        Monadic<String, Integer, Integer> m2 = (c, i) -> Future.succeededFuture(i + 2);

        m1.zipParCtx(m2, (c, a, b) -> a + b)
                .retry(vertx, Monadic.RetryPolicy.fast())
                .process(CTX, 10)
                .onComplete(vtc.succeeding(res -> {
                    vtc.verify(() -> assertEquals(23, res)); // (10+1) + (10+2)
                    vtc.completeNow();
                }));
    }
}
