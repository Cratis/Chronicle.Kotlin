```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason

/**
 * Redacts a single event by sequence number without stating a specific reason.
 */
suspend fun redactUnknown(store: IEventStore, sequenceNumber: Long) =
    store.eventLog.redact(EventSequenceNumber(sequenceNumber), RedactionReason.unknown)
```
