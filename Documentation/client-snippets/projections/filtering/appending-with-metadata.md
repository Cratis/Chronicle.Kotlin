```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import java.util.UUID

class FilteringAppendService(private val eventStore: IEventStore) {
    suspend fun appendOrders(customerId: String) {
        // Appends to all observers — no extra metadata
        eventStore.eventLog.append(
            UUID.randomUUID().toString(),
            FilteringWithReactorOrderPlaced(customerId, 42.0)
        )

        // Appends to all observers; additionally dispatched to observers filtering on "premium"
        eventStore.eventLog.append(
            UUID.randomUUID().toString(),
            FilteringWithReactorOrderPlaced(customerId, 299.0),
            AppendOptions(tags = listOf("premium"))
        )

        // Appends with stream type; dispatched to observers filtering on "wholesale" stream type
        eventStore.eventLog.append(
            UUID.randomUUID().toString(),
            FilteringWithReactorOrderPlaced(customerId, 1500.0),
            AppendOptions(eventStreamType = "wholesale")
        )
    }
}
```
