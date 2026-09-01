```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PagedEndpointOrder(val status: String = "", val total: Double = 0.0)

/**
 * A paged read suitable for backing a list endpoint - only the requested page is loaded.
 */
suspend fun getOrders(store: IEventStore, page: Int, pageSize: Int): List<PagedEndpointOrder> =
    store.readModels.materialized.getInstances(PagedEndpointOrder::class, skip = page * pageSize, take = pageSize)
```
