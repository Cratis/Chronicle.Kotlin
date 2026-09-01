```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class SnapshotsAnalyzeOrder(val status: String = "", val total: Double = 0.0)

suspend fun analyzeSnapshots(store: IEventStore, orderId: String) {
    val snapshots = store.readModels.getSnapshotsById(SnapshotsAnalyzeOrder::class, orderId)
    snapshots.forEach { snapshot ->
        println("Snapshot at ${snapshot.occurred}:")
        println("  Correlation ID: ${snapshot.correlationId}")
        println("  Event count: ${snapshot.events.size}")
        println("  State: ${snapshot.instance}")
    }
}
```
