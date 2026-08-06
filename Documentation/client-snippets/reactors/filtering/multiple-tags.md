```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactors-filtering-multi-tag-order-placed")
data class ReactorsFilteringMultiTagOrderPlaced(val totalAmount: Double)

@Reactor
@FilterEventsByTag("priority")
@FilterEventsByTag("express")
class ReactorsFilteringFastTrackOrderNotifier {
    fun placed(
        event: ReactorsFilteringMultiTagOrderPlaced,
        context: EventContext
    ) {
        // Only events appended with both tags reach this handler.
    }
}
```
