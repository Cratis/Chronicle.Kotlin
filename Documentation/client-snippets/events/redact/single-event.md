```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason

/**
 * Permanently rewrites the content of a single event. This is destructive — the original
 * content is gone once this returns — so only redact after a confirmed compliance request.
 */
suspend fun redactAddressEvent(store: IEventStore, sequenceNumber: Long) {
    store.eventLog.redact(EventSequenceNumber(sequenceNumber), RedactionReason("GDPR erasure request"))
}
```
