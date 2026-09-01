```java title="Update an audit timestamp from every event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "inventory-product-registered-for-every")
record InventoryProductRegisteredForEvery(String productName) {}

@EventType(id = "inventory-items-adjusted-for-every")
record InventoryItemsAdjustedForEvery(int quantity) {}

@ReadModel
@FromEvent(eventType = InventoryProductRegisteredForEvery.class)
@FromEvent(eventType = InventoryItemsAdjustedForEvery.class)
class InventoryStatusFromEvery {
    public String productName = "";

    @FromEvery(contextProperty = "occurred")
    public String lastUpdated = "";
}
```
