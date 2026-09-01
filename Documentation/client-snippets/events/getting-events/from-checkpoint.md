```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * Replays every event from a known checkpoint onwards, across all event sources - useful for
 * rebuilding projections or read models from a saved position.
 */
suspend fun readFrom(store: IEventStore, sequenceNumber: Long): List<AppendedEvent> =
    store.eventLog.getFromSequenceNumber(EventSequenceNumber(sequenceNumber))
```
