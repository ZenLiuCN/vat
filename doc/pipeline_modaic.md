# Pipeline.Monadic

---

## 1. The Monadic Blueprint

The `Monadic<C, I, O>` is a functional interface. It doesn't execute immediately; it defines a **blueprint** for an
asynchronous process.

* **C (Context):** Shared state (e.g., Auth, Database Transaction, Request ID).
* **I (Input):** The value entering this specific stage.
* **O (Output):** The value produced by this stage, wrapped in a `Future`.

---

## 2. Core Transformation & Composition

These methods form the backbone of the pipeline, allowing you to chain operations.

| Method                       | Description                                                                               |
|------------------------------|-------------------------------------------------------------------------------------------|
| **`identity()`**             | The "No-Op" pipe. Returns the input as the output.                                        |
| **`from(BiFunction)`**       | Lift a standard `(Context, Input) -> Future<Output>` function into the Monadic world.     |
| **`map(Function)`**          | Transforms the result  synchronously.                                                     |
| **`mapCtx(BiFunction)`**     | Transforms the result using both the Context and the Output.                              |
| **`mapOpt / mapOptCtx`**     | Transforms the result into an `Optional<R>`, handling nulls safely.                       |
| **`flatMap(Function)`**      | Chains an async operation .                                                               |
| **`flatMapCtx(BiFunction)`** | Chains an async operation with access to the Context.                                     |
| **`andThen(Monadic)`**       | Connects two Monadic pipes where the output of the first matches the input of the second. |
| **`value(R) / valueCtx`**    | Overwrites the current pipeline output with a constant or context-based value.            |

---

## 3. Resilience & Control Flow

Methods designed to handle timing, retries, and failures.

* **`retry(Vertx, RetryPolicy)`**: Re-runs the entire upstream pipe according to the `RetryPolicy` (Exponential
  backoff + Jitter).
* **`timeout(long, TimeUnit)`**: Enforces a time limit on the operation.
* **`withBreaker(CircuitBreaker, Function)`**: Protects a specific `flatMap` step with a circuit breaker.
* **`delay(Vertx, long, TimeUnit)`**: Artificially pauses the pipeline before completing the next step.
* **`sticky(Vertx)`**: Forces the pipeline to resume on the current Vert.x Event Loop context.

---

## 4. Error Handling & Recovery

Comprehensive tools for managing the "unhappy path."

* **`onError(Handler) / onErrorCtx`**: Side-effect callbacks triggered on failure.
* **`recover(Function) / recoverCtx`**: Catches an error and provides a fallback `Future`.
* **`recover(Class<E>, ...)`**: Type-specific recovery (e.g., only recover if it's a `DatabaseException`).
* **`recover(Predicate, ...)`**: Conditional recovery based on the error's properties.
* **`eventually(Supplier) / eventuallyCtx`**: Executes logic regardless of success or failure (similar to `finally`).
* **`fold(onSuccess, onFailure) / foldCtx`**: Converges success and failure paths into a single result type.

---

## 5. Parallelism & Branching

Utilities for executing multiple tasks simultaneously.

* **`zipPar(other, combiner)`**: Runs this pipe and another pipe in parallel, combining results when both finish.
* **`zipParCtx(...)`**: Parallel execution with context access for the combiner.
* **`race(competitors...) / raceWith`**: Executes multiple pipes; the first one to succeed wins, and its result is
  returned.
* **`match(Predicate, onTrue, onFalse)`**: Asynchronous branching (if/else) logic.
* **`flatMapIf(Predicate, action)`**: Only executes the async action if the predicate matches; otherwise, skips it.

---

## 6. Validation & Resource Management

* **`should(Expectation)`**: Asserts a condition; fails the pipe if not met.
* **`guard(Predicate, DomainError)`**: Manual check that fails the pipe with a specific `DomainError`.
* **`check(Predicate, validation)`**: Runs an external async validation (like a DB check) without changing the pipeline
  value.
* **`bracket(use, release)`**: Safe resource management. Ensures `release` is called even if `use` fails.

---

## 7. Batch Processing (`Monadic.Batch`)

When a pipeline output is a collection, `asBatch()` enters a specialized mode for element-wise processing.

| Batch Method                   | Description                                                                  |
|--------------------------------|------------------------------------------------------------------------------|
| **`mapEach / mapEachCtx`**     | Synchronous transformation of every element.                                 |
| **`mapEachPar`**               | Asynchronous parallel transformation of all elements.                        |
| **`mapEachPar(int, ...)`**     | **Concurrency-limited** parallel mapping (prevents overloading resources).   |
| **`filter / filterCtx`**       | Synchronous filtering of the collection.                                     |
| **`filterPar / filterParCtx`** | Asynchronous filtering (e.g., check "isDeleted" status in DB for each item). |
| **`group / groupCtx`**         | Groups elements into a `Map<K, List<E>>`.                                    |
| **`reduce / reduceCtx`**       | Folds the collection into a single value.                                    |
| **`collect(Function)`**        | Finalizes the batch by applying a collector function.                        |

---

## 8. Infrastructure Details

* **`withContext(Function)`**: Maps a parent context to a child context (Context Narrowing).
* **`blocking(Vertx, Function)`**: Wraps a step in `executeBlocking` to handle CPU-intensive or legacy blocking tasks.
* **`Steps` Implementation**: Internally uses a `LinkedList` of functions to avoid deep recursion and
  `StackOverflowError`, transforming the chain into a sequential loop over `AsyncResult`.

