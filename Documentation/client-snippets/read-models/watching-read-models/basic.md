```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.flow.collect

@ReadModel
data class WatchingBasicOrder(val status: String = "", val totalAmount: Double = 0.0)

suspend fun watchOrders(store: IEventStore) {
    store.readModels.watch(WatchingBasicOrder::class).collect { changeset ->
        if (changeset.removed || changeset.readModel == null) return@collect

        println("${changeset.modelKey}: ${changeset.readModel!!.status}")
    }
}
```
