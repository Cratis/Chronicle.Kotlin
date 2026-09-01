```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant
import java.util.UUID

@EventType(id = "event-processing-context-order-placed")
data class EventProcessingContextOrderPlaced(val orderId: UUID, val amount: Double)

@ReadModel
data class EventProcessingOrderSummaryWithContext(
    val orderId: UUID = UUID(0, 0),
    val total: Double = 0.0,
    val placedAt: Instant = Instant.EPOCH,
    val placedBy: String = "",
    val correlationId: UUID = UUID(0, 0)
)

@Reducer
class EventProcessingOrderSummaryWithContextReducer {
    fun placed(event: EventProcessingContextOrderPlaced, current: EventProcessingOrderSummaryWithContext?, context: EventContext) =
        EventProcessingOrderSummaryWithContext(
            orderId = event.orderId,
            total = event.amount,
            placedAt = context.occurred,
            placedBy = context.causedBy.subject,
            correlationId = context.correlationId
        )
}
```
