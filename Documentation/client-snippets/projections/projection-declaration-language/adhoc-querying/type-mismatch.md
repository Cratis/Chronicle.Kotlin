```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.projections.ProjectionQueryResult

suspend fun queryBad(store: IEventStore) {
    // This declaration comes back as ProjectionQueryResult.Invalid:
    // OrderPlaced.value is a string, but OrderShipped.value is an int
    val result = store.projections.query(
        """
        projection Bad
          from OrderPlaced   // value: string
          from OrderShipped  // value: int  -> incompatible types
        """
    )

    if (result is ProjectionQueryResult.Invalid) {
        result.errors.forEach { println(it) }
    }
}
```
