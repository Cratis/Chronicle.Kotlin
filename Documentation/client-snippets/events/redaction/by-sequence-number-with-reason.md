```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason

/**
 * Redacts a single event by sequence number with a meaningful reason.
 */
suspend fun redactWithReason(store: IEventStore, sequenceNumber: Long) =
    store.eventLog.redact(EventSequenceNumber(sequenceNumber), RedactionReason("GDPR erasure request"))
```
