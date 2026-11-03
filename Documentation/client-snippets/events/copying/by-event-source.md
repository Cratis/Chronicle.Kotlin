```kotlin
import io.cratis.chronicle.IEventStore

/**
 * Copies every event of one event source onto another. The copy runs as a durable job on the
 * kernel; the returned job id identifies it.
 */
suspend fun copyAccountHistory(store: IEventStore, sourceId: String, targetId: String): String =
    store.eventLog.copyForEventSource(sourceId, targetId)
```
