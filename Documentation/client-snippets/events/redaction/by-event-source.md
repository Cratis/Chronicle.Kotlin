```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.RedactionReason

/**
 * Redacts every event associated with a particular event source - for example, to erase all
 * data for a specific user.
 */
suspend fun redactAccount(store: IEventStore, eventSourceId: String) =
    store.eventLog.redactForEventSource(eventSourceId, RedactionReason("Account deletion requested"))
```
