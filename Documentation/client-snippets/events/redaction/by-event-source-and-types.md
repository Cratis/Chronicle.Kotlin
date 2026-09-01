```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.RedactionReason

@EventType
data class RedactionPersonalDetailsRecorded(val name: String = "", val socialSecurityNumber: String = "")

@EventType
data class RedactionAddressChanged(val street: String = "", val city: String = "")

/**
 * Redacts only the given event types for an event source, leaving every other event type intact.
 */
suspend fun redactPersonalData(store: IEventStore, eventSourceId: String) =
    store.eventLog.redactForEventSource(
        eventSourceId,
        RedactionReason("PII erasure"),
        listOf(RedactionPersonalDetailsRecorded::class, RedactionAddressChanged::class)
    )
```
