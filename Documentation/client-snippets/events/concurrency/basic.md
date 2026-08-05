```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConcurrencyStockReserved(val sku: String = "", val quantity: Int = 0)

/**
 * Appends only if the event source is still at the expected sequence number — the kernel
 * rejects the append with a concurrency violation if another writer got there first.
 */
suspend fun reserveStockIfUnchanged(store: IEventStore, sku: String, expectedSequenceNumber: Long) {
    val scope = ConcurrencyScopeBuilder()
        .withSequenceNumber(EventSequenceNumber(expectedSequenceNumber))
        .withEventSourceId()
        .build()

    val result = store.eventLog.append(sku, ConcurrencyStockReserved(sku, 1), AppendOptions(concurrencyScope = scope))
    val violation = result.concurrencyViolation
    if (!result.isSuccess && violation != null) {
        println("Concurrency violation: expected ${violation.expectedSequenceNumber}, actual ${violation.actualSequenceNumber}")
    }
}
```
