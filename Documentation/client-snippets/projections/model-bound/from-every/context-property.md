```kotlin title="Update an audit timestamp from every event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "inventory-product-registered-for-every")
data class InventoryProductRegisteredForEvery(val productName: String)

@EventType(id = "inventory-items-adjusted-for-every")
data class InventoryItemsAdjustedForEvery(val quantity: Int)

@ReadModel
@FromEvent(InventoryProductRegisteredForEvery::class)
@FromEvent(InventoryItemsAdjustedForEvery::class)
data class InventoryStatusFromEvery(
    val productName: String = "",

    @FromEvery(contextProperty = "occurred")
    val lastUpdated: String = ""
)
```
