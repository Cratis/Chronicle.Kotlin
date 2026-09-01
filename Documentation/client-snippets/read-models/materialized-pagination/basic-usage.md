```kotlin
import io.cratis.chronicle.IEventStore

class MaterializedPaginationBasicUsage(private val eventStore: IEventStore) {
    suspend fun getOrders(): List<MaterializedPaginationOrder> =
        eventStore.readModels.materialized.getInstances(MaterializedPaginationOrder::class)
}
```
