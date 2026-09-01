```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog

@EventType
data class ReducersFilteringOrderPlaced(val totalAmount: Double)

class ReducersFilteringMetadataExampleService(private val eventLog: IEventLog) {
    suspend fun placeOrder(eventSourceId: String, totalAmount: Double) =
        eventLog.append(
            eventSourceId,
            ReducersFilteringOrderPlaced(totalAmount),
            AppendOptions(
                tags = listOf("priority"),
                eventSourceType = "order",
                eventStreamType = "fulfillment"
            )
        )
}
```
