```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.flow.collect

@ReadModel
data class WatchAccountInfo(val name: String = "", val balance: Double = 0.0)

suspend fun watchAccountChanges(store: IEventStore) {
    store.readModels.watch(WatchAccountInfo::class).collect { changeset ->
        val label = if (changeset.removed) "removed" else "${changeset.readModel?.name}: ${changeset.readModel?.balance}"
        println("${changeset.modelKey} ${changeset.changeType}: $label")
    }
}
```
