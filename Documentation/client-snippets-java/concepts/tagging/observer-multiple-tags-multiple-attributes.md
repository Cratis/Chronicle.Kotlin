```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag("Integration")
@Tag("ExternalAPI")
@Tag("Inventory")
@Reactor
class TaggingInventorySyncReactor {
    void on(TaggingInventoryAdjusted event) { }
}

@EventType
record TaggingInventoryAdjusted(String id) {}
```
