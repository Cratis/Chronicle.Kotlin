```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@EventType
record TaggingReactorsProductStockChanged(String productId, int newQuantity) {}

interface TaggingReactorsInventoryApi {
    void updateStock(String productId, int newQuantity);
}

@Tag("Integration")
@Tag("ExternalAPI")
@Tag("Inventory")
@Reactor
class TaggingReactorsInventorySyncReactor {
    private final TaggingReactorsInventoryApi inventoryApi;

    TaggingReactorsInventorySyncReactor(TaggingReactorsInventoryApi inventoryApi) {
        this.inventoryApi = inventoryApi;
    }

    void stockChanged(TaggingReactorsProductStockChanged event, EventContext context) {
        inventoryApi.updateStock(event.productId(), event.newQuantity());
    }
}
```
