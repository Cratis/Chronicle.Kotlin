```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.flow.collect

data class ScenariosWatchBook(val title: String, val onLoan: Boolean)

class ScenariosQueryBookWatcher(private val store: IEventStore) {
    suspend fun watch() {
        store.readModels.watch(ScenariosWatchBook::class).collect { changeset ->
            if (changeset.removed || changeset.readModel == null) return@collect
            println("${changeset.modelKey}: on loan = ${changeset.readModel!!.onLoan}")
        }
    }
}
```
