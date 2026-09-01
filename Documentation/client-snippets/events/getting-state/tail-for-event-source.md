```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * Scopes the tail sequence number to a specific event source, rather than the whole event log.
 */
suspend fun captureFor(store: IEventStore, inventoryId: String): EventSequenceNumber =
    store.eventLog.getTailSequenceNumber(eventSourceId = inventoryId)
```
