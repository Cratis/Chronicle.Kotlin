```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyMonthlyReportGenerated(val month: String = "")

/**
 * Scopes concurrency to a specific event stream id within a stream type, so reports for
 * different months don't contend with each other.
 */
suspend fun generateMonthlyReport(store: IEventStore, accountId: String, monthKey: String) {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withEventStreamType("Reporting")
        .withEventStreamId(monthKey)
        .withSequenceNumber(EventSequenceNumber(5))
        .build()

    store.eventLog.append(
        accountId,
        ConcurrencyMonthlyReportGenerated(monthKey),
        AppendOptions(eventStreamType = "Reporting", eventStreamId = monthKey, concurrencyScope = concurrencyScope)
    )
}
```
