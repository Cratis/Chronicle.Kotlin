```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.events.EventType
import java.time.Instant

@EventType
data class OccurredOrderPlaced(val customerId: String = "", val total: Double = 0.0)

/**
 * Appends an event with an explicit [AppendOptions.occurred] timestamp, bypassing the kernel's
 * default of assigning the current server time. Use this only when importing or replaying
 * historical events.
 */
suspend fun placeHistoricalOrder(store: IEventStore, eventSourceId: String, customerId: String, total: Double) =
    store.eventLog.append(
        eventSourceId,
        OccurredOrderPlaced(customerId, total),
        AppendOptions(occurred = Instant.parse("2024-01-15T10:30:00Z"))
    )
```
