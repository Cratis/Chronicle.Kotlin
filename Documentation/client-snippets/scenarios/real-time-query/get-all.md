```kotlin
import io.cratis.chronicle.IEventStore

data class ScenariosQueryAllBook(val title: String, val onLoan: Boolean)

class ScenariosQueryOnLoanBooks(private val store: IEventStore) {
    suspend fun getOnLoan(): List<ScenariosQueryAllBook> =
        store.readModels.getInstances(ScenariosQueryAllBook::class).filter { it.onLoan }
}
```
