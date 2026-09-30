```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyFirstAccountOpened(val accountName: String = "")

/**
 * Opts in to the first-append check with [ConcurrencyScopeBuilder.withExpectsNoMatchingEvent].
 * The kernel rejects the append with a concurrency violation if an event already exists for
 * this event source, so two concurrent "open account" appends cannot both succeed.
 */
suspend fun openAccountOnce(store: IEventStore, accountId: String, accountName: String): Boolean {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withExpectsNoMatchingEvent()
        .build()

    val result = store.eventLog.append(
        accountId,
        ConcurrencyFirstAccountOpened(accountName),
        AppendOptions(concurrencyScope = concurrencyScope)
    )

    return result.isSuccess
}
```
