```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class SnapshotAccountInfo(val name: String = "", val balance: Double = 0.0)

/**
 * Gets the full history of intermediate states for a read model instance, grouped by correlation
 * id — unlike [io.cratis.chronicle.readModels.IReadModelsService.getInstanceByKey], which only
 * returns the latest state.
 */
suspend fun getAccountSnapshotHistory(store: IEventStore, accountId: String) {
    val snapshots = store.readModels.getSnapshotsById(SnapshotAccountInfo::class, accountId)
    snapshots.forEach { snapshot ->
        println("${snapshot.occurred}: ${snapshot.instance.name} -> ${snapshot.instance.balance}")
    }
}
```
