```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ReadModel
data class MaterializedPaginationProduct(val name: String = "", val price: Double = 0.0)

/**
 * Observes a live paginated window of materialized instances for as long as [scope] is active.
 */
class ProductDashboard(store: IEventStore, scope: CoroutineScope) {
    init {
        scope.launch {
            store.readModels.materialized.observeInstances(MaterializedPaginationProduct::class, take = 100)
                .collect { products -> updateView(products) }
        }
    }

    private fun updateView(products: List<MaterializedPaginationProduct>) {
        // ...
    }
}
```
