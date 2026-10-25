# Correlation identifiers

Chronicle stamps every append with a correlation identifier. When you do not
pass one in `AppendOptions.correlationId`, the client falls back to the
current thread's ambient identifier held by `correlationIdManager`.

## The shared singleton

`correlationIdManager` is bound to `CorrelationIdContext.shared()` from
`io.cratis:fundamentals`. Reading, setting or clearing through either one
reads, sets or clears the same per-thread value.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.correlation.correlationIdManager
import io.cratis.fundamentals.execution.CorrelationId
import io.cratis.fundamentals.execution.CorrelationIdContext
import java.util.UUID

fun bindSharedCorrelation() {
    val id = UUID.randomUUID()
    correlationIdManager.set(id)
    check(CorrelationIdContext.shared().current == CorrelationId(id))
    correlationIdManager.clear() // the next read generates a new identifier
}
```

## Isolated managers

`CorrelationIdManager()` creates a manager with its own independent state.
It shares nothing with other managers or with the singleton.

## Thread boundary and cleanup

- The value is stored per thread and is **not** inherited by threads you
  start or by executor workers.
- The first read on a thread without a value generates and retains a UUID.
  An explicitly set zero UUID is kept as is.
- Nothing is propagated across coroutine suspension or dispatcher changes,
  and there is no scope stack or automatic restoration. Whoever sets a value
  is responsible for calling `clear()`, as the Spring Boot unit-of-work
  filter does.

## Suspending work

For suspending or multi-threaded work, capture the identifier at the entry
point and pass it explicitly:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventSequence
import java.util.UUID

suspend fun appendWithExplicitCorrelation(
    sequence: IEventSequence,
    event: Any,
    id: UUID
) {
    sequence.append("source-1", event, AppendOptions(correlationId = id))
}
```

An explicit `AppendOptions.correlationId` always takes precedence over the
ambient value and does not change it.
