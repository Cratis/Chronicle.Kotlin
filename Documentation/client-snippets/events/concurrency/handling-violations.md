```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencySafeAccountOpened(val accountName: String = "")

/**
 * Handles a concurrency violation reported on [io.cratis.chronicle.eventSequences.AppendResult.concurrencyViolation]
 * by reporting which event source lost the race. The scope expects that no event exists yet
 * for the account, so a second "open account" append fails with a violation.
 */
suspend fun tryOpenAccount(store: IEventStore, accountId: String, accountName: String): Boolean {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withExpectsNoMatchingEvent()
        .build()

    val result = store.eventLog.append(
        accountId,
        ConcurrencySafeAccountOpened(accountName),
        AppendOptions(concurrencyScope = concurrencyScope)
    )

    val violation = result.concurrencyViolation
    if (violation != null) {
        println("Concurrency violation for event source ${violation.eventSourceId}")
        return false
    }

    return result.isSuccess
}
```
