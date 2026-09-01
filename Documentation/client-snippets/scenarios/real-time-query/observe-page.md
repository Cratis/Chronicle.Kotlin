```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.flow.Flow

data class ScenariosObserveBook(val title: String, val onLoan: Boolean)

class ScenariosQueryLiveBookPage(private val store: IEventStore) {
    fun subscribe(): Flow<List<ScenariosObserveBook>> =
        store.readModels.materialized.observeInstances(ScenariosObserveBook::class, skip = 0, take = 50)
}
```
