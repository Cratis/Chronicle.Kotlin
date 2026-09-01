```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@EventType
data class TaggingReactorsProductStockChanged(val productId: String, val newQuantity: Int)

interface TaggingReactorsInventoryApi {
    suspend fun updateStock(productId: String, newQuantity: Int)
}

@Tag("Integration")
@Tag("ExternalAPI")
@Tag("Inventory")
@Reactor
class TaggingReactorsInventorySyncReactor(private val inventoryApi: TaggingReactorsInventoryApi) {
    suspend fun stockChanged(event: TaggingReactorsProductStockChanged, context: EventContext) =
        inventoryApi.updateStock(event.productId, event.newQuantity)
}
```
