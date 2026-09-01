```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventType(id = "event-processing-reuse-item-added")
record EventProcessingReuseItemAdded(UUID itemId, String name) {}

record EventProcessingItem(UUID itemId, String name) {}

@ReadModel
record EventProcessingItemList(List<EventProcessingItem> items) {
    EventProcessingItemList() {
        this(List.of());
    }
}

@Reducer
class EventProcessingItemListReducer {
    EventProcessingItemList itemAdded(EventProcessingReuseItemAdded event, EventProcessingItemList current) {
        // Copy rather than mutate current.items() directly - a held snapshot may still reference it
        List<EventProcessingItem> items = new ArrayList<>(current == null ? List.of() : current.items());
        items.add(new EventProcessingItem(event.itemId(), event.name()));

        return new EventProcessingItemList(items);
    }
}
```
