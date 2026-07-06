```kotlin
import io.cratis.chronicle.IEventStore

data class ScenariosQueryBook(val title: String, val onLoan: Boolean)

class ScenariosQueryBookService(private val store: IEventStore) {
    suspend fun getBook(bookId: String): ScenariosQueryBook? =
        store.readModels.getInstanceByKey(ScenariosQueryBook::class, bookId)
}
```
