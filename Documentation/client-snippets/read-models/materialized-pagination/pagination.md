```kotlin
import io.cratis.chronicle.IEventStore

class MaterializedPaginationPagination(private val eventStore: IEventStore) {
    suspend fun getPages() {
        // First page of 20
        val page1 = eventStore.readModels.materialized.getInstances(MaterializedPaginationOrder::class, take = 20)
        println("Page 1: ${page1.size} orders")

        // Second page of 20
        val page2 = eventStore.readModels.materialized.getInstances(MaterializedPaginationOrder::class, skip = 20, take = 20)
        println("Page 2: ${page2.size} orders")

        // Third page of 20
        val page3 = eventStore.readModels.materialized.getInstances(MaterializedPaginationOrder::class, skip = 40, take = 20)
        println("Page 3: ${page3.size} orders")
    }
}
```
