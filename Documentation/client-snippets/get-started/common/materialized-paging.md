```kotlin
import io.cratis.chronicle.IEventStore

class GetStartedMaterializedBooks(private val eventStore: IEventStore) {
    suspend fun page(skip: Int = 0, take: Int = 50): List<GetStartedBookDetails> =
        eventStore.readModels.materialized.getInstances(GetStartedBookDetails::class, skip, take)
}
```
