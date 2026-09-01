```kotlin
import io.cratis.chronicle.IEventStore

data class ScenariosMaterializedBook(val title: String, val onLoan: Boolean)

class ScenariosQueryBookPage(private val store: IEventStore) {
    suspend fun getPage(): List<ScenariosMaterializedBook> =
        store.readModels.materialized.getInstances(ScenariosMaterializedBook::class, skip = 0, take = 20)
}
```
