```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class CopyingAccountOpened(val owner: String = "")

@EventType
data class CopyingAddressChanged(val street: String = "", val city: String = "")

/**
 * Copies only the given event types for an event source, leaving every other event type behind.
 */
suspend fun copyAccountDetails(store: IEventStore, sourceId: String, targetId: String): String =
    store.eventLog.copyForEventSource(
        sourceId,
        targetId,
        listOf(CopyingAccountOpened::class, CopyingAddressChanged::class)
    )
```
