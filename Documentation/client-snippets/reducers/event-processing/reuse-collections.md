```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.util.UUID

@EventType(id = "event-processing-reuse-item-added")
data class EventProcessingReuseItemAdded(val itemId: UUID, val name: String)

data class EventProcessingItem(val itemId: UUID, val name: String)

@ReadModel
data class EventProcessingItemList(val items: List<EventProcessingItem> = emptyList())

@Reducer
class EventProcessingItemListReducer {
    fun itemAdded(event: EventProcessingReuseItemAdded, current: EventProcessingItemList?): EventProcessingItemList {
        // Build a new list rather than mutate current.items directly - a held snapshot may still
        // reference it
        val items = (current?.items ?: emptyList()) + EventProcessingItem(event.itemId, event.name)

        return EventProcessingItemList(items)
    }
}
```
