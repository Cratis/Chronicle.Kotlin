```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.projections.ProjectionQueryResult

suspend fun queryOrders(store: IEventStore) {
    // A declaration that fails to parse is not an exception — it comes back as a result you branch on.
    when (val result = store.projections.query(
        """
        projection Orders
          from OrderPlaced
        """
    )) {
        is ProjectionQueryResult.Projected -> result.entries.forEach(::println)
        is ProjectionQueryResult.Invalid -> result.errors.forEach { println(it) }
    }
}
```
