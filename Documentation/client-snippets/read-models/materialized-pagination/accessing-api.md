```kotlin
import io.cratis.chronicle.IEventStore

data class MaterializedPaginationOrder(val customerName: String = "", val total: Double = 0.0)

class MaterializedPaginationAccessingApi(private val eventStore: IEventStore) {
    // Reach through IEventStore, then the Materialized API
    suspend fun getOrders(): List<MaterializedPaginationOrder> =
        eventStore.readModels.materialized.getInstances(MaterializedPaginationOrder::class)
}
```
