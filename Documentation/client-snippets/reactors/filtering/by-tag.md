```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactors-filtering-by-tag-order-placed")
data class ReactorsFilteringByTagOrderPlaced(val totalAmount: Double)

class ReactorsFilteringByTagOrderService(private val eventLog: IEventLog) {
    suspend fun placePriorityOrder(eventSourceId: String, totalAmount: Double) =
        eventLog.append(
            eventSourceId,
            ReactorsFilteringByTagOrderPlaced(totalAmount),
            AppendOptions(tags = listOf("priority"))
        )
}

@Reactor
@FilterEventsByTag("priority")
class ReactorsFilteringPriorityOrderNotifier {
    fun placed(event: ReactorsFilteringByTagOrderPlaced, context: EventContext) {
    }
}
```
