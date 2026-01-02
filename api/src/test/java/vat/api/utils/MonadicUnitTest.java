package vat.api.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import vat.api.DomainError;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unified Test Suite for Monadic utility to ensure maximum coverage.
 * Covers basic operators, batch processing, resilience, and internal optimizations.
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@NullMarked
class MonadicUnitTest {

    private static final String CTX = "TestContext";

    // --- 1. CORE OPERATORS & TRANSFORMS ---

    @Test
    @Order(1)
    @DisplayName("Test Basic Transform Operators (map, flatMap, peek, value)")
    void testBasicOperators(VertxTestContext testContext) {
        AtomicInteger peekSideEffect = new AtomicInteger(0);

        Monadic.<String, Integer>identity()
               .map(i -> i * 2)
               .peek(peekSideEffect::set)
               .value("FixedValue")
               .process(CTX, 10)
               .onComplete(testContext.succeeding(result -> {
                   testContext.verify(() -> {
                       assertEquals("FixedValue", result);
                       assertEquals(20, peekSideEffect.get());
                   });
                   testContext.completeNow();
               }));
    }

    @Test
    @Order(2)
    @DisplayName("Test Optional Mapping and Context-aware Mapping")
    void testMapOptAndCtx(VertxTestContext vtc) {
        Monadic.<String, String>identity()
               .mapOpt((ctx, s) -> s.length() > 3 ? s + ctx : null)
               .process("!", "hi") // Too short -> Optional.empty
               .flatMap(res1 ->
                                Monadic.<String, String>identity()
                                       .mapOpt((ctx, s) -> s.length() > 3 ? s + ctx : null)
                                       .process("!", "hello") // Pass -> Optional.of("hello!")
                                       .map(res2 -> List.of(res1, res2))
                       )
               .onComplete(vtc.succeeding(results -> {
                   vtc.verify(() -> {
                       assertEquals(Optional.empty(), results.get(0));
                       assertEquals(Optional.of("hello!"), results.get(1));
                   });
                   vtc.completeNow();
               }));
    }

    @Test
    @Order(3)
    @DisplayName("Verify Pipeline Merging (andThen)")
    void testMergedPipeline(VertxTestContext vtc) {
        Monadic<String, Integer, Integer> step1 = Monadic.<String, Integer>identity().map(i -> i + 100);
        Monadic<String, Integer, String> step2 = Monadic.<String, Integer>identity().map(i -> "Val:" + i);

        step1.andThen(step2).process(CTX, 5).onComplete(vtc.succeeding(result -> {
            vtc.verify(() -> assertEquals("Val:105", result));
            vtc.completeNow();
        }));
    }

    // --- 2. RESILIENCE & ERROR HANDLING ---

    @Test
    @Order(4)
    @DisplayName("Test Recovery Logic (Typed, Conditional, and Sync failure)")
    void testRecoveryFlows(VertxTestContext vtc) {
        Monadic.<String, String>identity()
               .map(in -> {throw new IllegalArgumentException("Sync Fail");})
               .recover(IllegalArgumentException.class, err -> Future.succeededFuture("CaughtTyped"))
               .recover((ctx, err) -> Future.succeededFuture("Fallback"))
               .process(CTX, "input")
               .onComplete(vtc.succeeding(result -> {
                   vtc.verify(() -> assertEquals("CaughtTyped", result));
                   vtc.completeNow();
               }));
    }

    @Test
    @Order(5)
    @DisplayName("Guard and DomainError short-circuiting")
    void testGuard(VertxTestContext vtc) {
        Monadic.<String, Integer>identity()
               .guard(i -> i > 10, DomainError.System.badRequest("Too small"))
               .process(CTX, 5)
               .onComplete(vtc.failing(err -> {
                   vtc.verify(() -> {
                       assertTrue(err instanceof DomainError);
                       assertEquals("Too small", err.getMessage());
                   });
                   vtc.completeNow();
               }));
    }

    @Test
    @Order(6)
    @DisplayName("Retry Logic with Exponential Backoff")
    void testRetry(Vertx vertx, VertxTestContext vtc) {
        AtomicInteger attempts = new AtomicInteger(0);
        Monadic.<String, String, String>from((c, i) -> {
                   if (attempts.incrementAndGet() < 3) return Future.failedFuture("Retry Me");
                   return Future.succeededFuture("Success");
               })
               .retry(vertx, new Monadic.RetryPolicy(3, 10, 100, t -> true))
               .process(CTX, "in")
               .onComplete(vtc.succeeding(res -> {
                   vtc.verify(() -> assertEquals(3, attempts.get()));
                   vtc.completeNow();
               }));
    }

    // --- 3. BATCH OPERATIONS ---

    @Test
    @Order(7)
    @DisplayName("Batch: Parallel Mapping with Concurrency Limit")
    void testBatchParallel(Vertx vertx, VertxTestContext vtc) {
        List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5);
        Monadic.<String, List<Integer>>identity()
               .asBatch(l -> l)
               .mapEachPar(2, i -> Future.future(p -> vertx.setTimer(10, id -> p.complete(i * 10))))
               .process(CTX, inputs)
               .onComplete(vtc.succeeding(result -> {
                   vtc.verify(() -> assertEquals(Arrays.asList(10, 20, 30, 40, 50), result));
                   vtc.completeNow();
               }));
    }

    @Test
    @Order(8)
    @DisplayName("Batch: Filtering, Grouping and Reduction")
    void testBatchUtility(VertxTestContext vtc) {
        Monadic.<String, List<Integer>>identity()
               .asBatch(l -> l)
               .filter(i -> i % 2 != 0)
               .reduce(0, Integer::sum)
               .process(CTX, Arrays.asList(1, 2, 3, 4))
               .onComplete(vtc.succeeding(res -> {
                   vtc.verify(() -> assertEquals(4, res)); // 1 + 3
                   vtc.completeNow();
               }));
    }

    // --- 4. ADVANCED FLOW CONTROL ---

    @Test
    @Order(9)
    @DisplayName("Bracket: Resource Lifecycle Management")
    void testBracket(VertxTestContext vtc) {
        AtomicBoolean released = new AtomicBoolean(false);
        Monadic.<String, String>identity()
               .bracket(
                       res -> Future.succeededFuture("Used " + res),
                       res -> {released.set(true); return Future.succeededFuture();}
                       )
               .process(CTX, "Resource")
               .onComplete(vtc.succeeding(res -> {
                   vtc.verify(() -> {
                       assertEquals("Used Resource", res);
                       assertTrue(released.get());
                   });
                   vtc.completeNow();
               }));
    }

    @Test
    @Order(10)
    @DisplayName("Race and Timeout Handling")
    void testRaceAndTimeout(Vertx vertx, VertxTestContext vtc) {
        Monadic.<String, String>identity()
               .race(
                       in -> Future.future(p -> vertx.setTimer(100, id -> p.complete("Slow"))),
                       in -> Future.succeededFuture("Fast")
                    )
               .timeout(500, TimeUnit.MILLISECONDS)
               .process(CTX, "start")
               .onComplete(vtc.succeeding(result -> {
                   vtc.verify(() -> assertEquals("Fast", result));
                   vtc.completeNow();
               }));
    }

    // --- 5. INTERNAL LOGIC & OPTIMIZATION ---

    @Test
    @Order(11)
    @DisplayName("Test Finalization (Optimization to array-based steps)")
    void testFinalization(VertxTestContext vtc) {
        Monadic<String, Integer, Integer> pipeline = Monadic.<String, Integer>identity()
                                                            .map(i -> i + 1)
                                                            .finalization();

        assertTrue(pipeline instanceof Monadic.Finalized);
        pipeline.process(CTX, 10).onComplete(vtc.succeeding(res -> {
            vtc.verify(() -> assertEquals(11, res));
            vtc.completeNow();
        }));
    }

    @Test
    @Order(12)
    @DisplayName("Diagram Generation Coverage")
    void testDiagram() {
        String diagram = Monadic.identity().map(i -> i).diagram();
        assertNotNull(diagram);
        assertTrue(diagram.contains("graph TD"));

        // Test diagram on Steps internal class
        Monadic<String, Integer, Integer> steps = Monadic.<String, Integer>identity().map(i -> i);
        if (steps instanceof Monadic.Steps) {
            assertNotNull(((Monadic.Steps<?, ?, ?>) steps).diagram());
        }
    }

    @Test
    @Order(13)
    @DisplayName("Verify Sticky Context and Execution Blocking")
    void testThreadingUtils(Vertx vertx, VertxTestContext vtc) {
        Monadic.<String, Integer>identity()
               .sticky(vertx)
               .blocking(vertx, i -> i * 2)
               .process(CTX, 5)
               .onComplete(vtc.succeeding(res -> {
                   vtc.verify(() -> assertEquals(10, res));
                   vtc.completeNow();
               }));
    }
}
