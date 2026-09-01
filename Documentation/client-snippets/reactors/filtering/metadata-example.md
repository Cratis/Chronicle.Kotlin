```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog

@EventType(id = "reactors-filtering-order-placed")
data class ReactorsFilteringOrderPlaced(val totalAmount: Double)

class ReactorsFilteringMetadataExampleService(private val eventLog: IEventLog) {
    suspend fun placeOrder(eventSourceId: String, totalAmount: Double) =
        eventLog.append(
            eventSourceId,
            ReactorsFilteringOrderPlaced(totalAmount),
            AppendOptions(
                tags = listOf("priority"),
                eventSourceType = "order",
                eventStreamType = "fulfillment"
            )
        )
}
```
