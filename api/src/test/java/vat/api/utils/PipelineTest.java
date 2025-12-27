package vat.api.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class PipelineTest {

    @Test
    @DisplayName("Basic Pipeline mapping and context propagation")
    void testBasicMapping(VertxTestContext vtc) {
        String context = "ctx-1";
        Pipeline.with(context, 10)
                .map(i -> i * 2)
                .mapCtx((ctx, val) -> ctx + ":" + val)
                .get()
                .onComplete(vtc.succeeding(result -> {
                    vtc.verify(() -> {
                        assertEquals("ctx-1:20", result);
                        vtc.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Pipeline recovery from error")
    void testRecovery(VertxTestContext vtc) {
        Pipeline.of("ctx", Future.<Integer>failedFuture(new RuntimeException("Oops")))
                .recover(err -> 0)
                .get()
                .onComplete(vtc.succeeding(result -> {
                    vtc.verify(() -> {
                        assertEquals(0, result);
                        vtc.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Batch parallel mapping with concurrency limit")
    void testBatchParallelMapping(Vertx vertx, VertxTestContext vtc) {
        List<Integer> inputs = List.of(1, 2, 3);

        Pipeline.Batch.with("batch-ctx", inputs)
                      .mapEachPar(2, item -> vertx.executeBlocking(() -> {
                          Thread.sleep(20);
                          return item * 10;
                      }))
                      .get()
                      .onComplete(vtc.succeeding(result -> {
                          vtc.verify(() -> {
                              assertNotNull(result);
                              assertEquals(3, result.size());
                              assertTrue(result.containsAll(List.of(10, 20, 30)));
                              vtc.completeNow();
                          });
                      }));
    }

    @Test
    @DisplayName("Batch filtering and grouping")
    void testBatchOperations(VertxTestContext vtc) {
        List<String> data = List.of("apple", "banana", "apricot");

        Pipeline.Batch.with("ctx", data)
                      .filter(s -> s.startsWith("a"))
                      .group(String::length)
                      .get()
                      .onComplete(vtc.succeeding(result -> {
                          vtc.verify(() -> {
                              assertTrue(result.containsKey(5)); // apple
                              assertTrue(result.containsKey(7)); // apricot
                              assertEquals(1, result.get(5).size());
                              vtc.completeNow();
                          });
                      }));
    }

    @Test
    @DisplayName("Resilience Circuit Breaker integration")
    void testResilienceBreaker(Vertx vertx, VertxTestContext vtc) {
        CircuitBreaker breaker = CircuitBreaker.create("test-breaker", vertx,
                                                       new CircuitBreakerOptions().setMaxFailures(1));

        Pipeline.Resilience.with("res-ctx", "input")
                           .withBreaker(breaker, val -> Future.succeededFuture(val.toUpperCase()))
                           .get()
                           .onComplete(vtc.succeeding(result -> {
                               vtc.verify(() -> {
                                   assertEquals("INPUT", result);
                                   vtc.completeNow();
                               });
                           }));
    }

    @Test
    @DisplayName("Pipeline bracket for resource management")
    void testBracket(VertxTestContext vtc) {
        AtomicInteger resourceState = new AtomicInteger(0); // 0: closed, 1: open

        Pipeline.with("ctx", "resource")
                .peek(r -> resourceState.set(1))
                .bracket(
                        res -> Future.succeededFuture(res.length()),
                        res -> {
                            resourceState.set(0);
                            return Future.succeededFuture();
                        }
                        )
                .get()
                .onComplete(vtc.succeeding(result -> {
                    vtc.verify(() -> {
                        assertEquals(8, result);
                        assertEquals(0, resourceState.get());
                        vtc.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("ZipPar parallel execution")
    void testZipPar(VertxTestContext vtc) {
        var p1 = Pipeline.with("ctx", 1);
        var p2 = Pipeline.with("ctx", 2);

        p1.zipPar(p2, Integer::sum)
          .get()
          .onComplete(vtc.succeeding(result -> {
              vtc.verify(() -> {
                  assertEquals(3, result);
                  vtc.completeNow();
              });
          }));
    }
    @Test
    @DisplayName("Convert Pipeline to Batch and back to Pipeline via reduce")
    void testPipelineToBatchConversion(VertxTestContext testContext) {
        String ctx = "conversion-ctx";

        // 1. Start with a Pipeline (Single Value)
        Pipeline.with(ctx, "a,b,c")
                // 2. Map to a List and wrap in a Batch
                .flatMap(s -> Future.succeededFuture(List.of(s.split(","))))
                .map(list -> Pipeline.Batch.of(ctx, Future.succeededFuture(list)))
                // 3. Perform Batch operations
                .flatMap(batch -> batch
                                 .mapEach(String::toUpperCase)
                                 // 4. Convert back to Pipeline (Single Value) via reduce
                                 .reduce("", (acc, item) -> acc + item)
                                 .get()
                        )
                .get()
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        assertEquals("ABC", result);
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Convert Batch to Pipeline via grouping")
    void testBatchToPipelineGrouping(VertxTestContext testContext) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);

        Pipeline.Batch.with("ctx", numbers)
                      .group(n -> n % 2 == 0 ? "even" : "odd")
                      .get()
                      .onComplete(testContext.succeeding(result -> {
                          testContext.verify(() -> {
                              assertNotNull(result);
                              // The list [1, 2, 3, 4, 5, 6] has THREE even numbers: 2, 4, 6
                              assertEquals(3, result.get("even").size(), "Should have 3 even numbers (2, 4, 6)");
                              assertEquals(3, result.get("odd").size(), "Should have 3 odd numbers (1, 3, 5)");
                              testContext.completeNow();
                          });
                      }));
    }

    @Test
    @DisplayName("Resilience conversion from Pipeline and Batch")
    void testResilienceFromOtherTypes(VertxTestContext testContext) {
       var pipe = Pipeline.with("ctx", "hello");
        var batch = Pipeline.Batch.with("ctx", List.of(1));

        // Test factory methods in Resilience interface
        var resFromPipe = Pipeline.Resilience.from(pipe);
        var resFromBatch = Pipeline.Resilience.from(batch);

        Future.all(resFromPipe.get(), resFromBatch.get())
              .onComplete(testContext.succeeding(cf -> {
                  testContext.verify(() -> {
                      assertEquals("hello", cf.resultAt(0));
                      assertEquals(List.of(1), cf.resultAt(1));
                      testContext.completeNow();
                  });
              }));
    }
}