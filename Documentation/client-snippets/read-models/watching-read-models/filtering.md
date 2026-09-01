```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

@ReadModel
data class WatchingFilteringOrder(val status: String = "", val totalAmount: Double = 0.0)

/**
 * Filtering happens client-side - the server still sends every change for the read model type.
 */
suspend fun watchHighValueOrders(store: IEventStore, threshold: Double) {
    store.readModels.watch(WatchingFilteringOrder::class)
        .filter { (it.readModel?.totalAmount ?: 0.0) > threshold }
        .collect { changeset ->
            println("${changeset.modelKey}: ${changeset.readModel?.totalAmount}")
        }
}
```
