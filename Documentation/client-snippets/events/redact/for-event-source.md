```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.RedactionReason

/**
 * Permanently redacts every event for a single event source — a full "right to be forgotten"
 * erasure. Leave `eventTypes` empty (the default) to redact every event type for that source.
 */
suspend fun redactAllEventsForCustomer(store: IEventStore, customerId: String) {
    store.eventLog.redactForEventSource(customerId, RedactionReason("GDPR erasure request"))
}
```
