```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencySafeAccountOpened(val accountName: String = "")

/**
 * Handles a concurrency violation reported on [io.cratis.chronicle.eventSequences.AppendResult.concurrencyViolation]
 * by comparing the expected and actual sequence numbers the kernel found.
 */
suspend fun tryOpenAccount(store: IEventStore, accountId: String, accountName: String): Boolean {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withSequenceNumber(EventSequenceNumber.first)
        .build()

    val result = store.eventLog.append(
        accountId,
        ConcurrencySafeAccountOpened(accountName),
        AppendOptions(concurrencyScope = concurrencyScope)
    )

    val violation = result.concurrencyViolation
    if (violation != null) {
        println("Expected sequence ${violation.expectedSequenceNumber.value}, actual was ${violation.actualSequenceNumber.value}")
        return false
    }

    return result.isSuccess
}
```
