```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class SnapshotsBasicOrder(val status: String = "", val total: Double = 0.0)

suspend fun countSnapshots(store: IEventStore, orderId: String) {
    val snapshots = store.readModels.getSnapshotsById(SnapshotsBasicOrder::class, orderId)
    println("Found ${snapshots.size} snapshots.")
}
```
