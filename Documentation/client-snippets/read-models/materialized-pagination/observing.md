```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class MaterializedPaginationProduct(val name: String = "", val price: Double = 0.0)

class MaterializedPaginationObserving(private val eventStore: IEventStore) {
    suspend fun run() = coroutineScope {
        val subscription = launch {
            eventStore.readModels.materialized
                .observeInstances(MaterializedPaginationProduct::class, take = 50)
                .collect { products ->
                    // Called whenever the stored instances change
                    println("Products updated: ${products.size} in view")
                }
        }

        // Cancel when done to release the change stream
        subscription.cancel()
    }
}
```
