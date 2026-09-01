```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyAccountSettingsUpdated(val settings: String = "")

/**
 * Scopes concurrency to a specific event source type and event stream type, in addition to the
 * event source id.
 */
suspend fun updateAccountSettings(store: IEventStore, accountId: String, settings: String) {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withEventSourceType("BankAccount")
        .withEventStreamType("AccountManagement")
        .withSequenceNumber(EventSequenceNumber(10))
        .build()

    store.eventLog.append(
        accountId,
        ConcurrencyAccountSettingsUpdated(settings),
        AppendOptions(eventSourceType = "BankAccount", eventStreamType = "AccountManagement", concurrencyScope = concurrencyScope)
    )
}
```
