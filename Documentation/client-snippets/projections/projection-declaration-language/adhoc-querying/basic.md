```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.projections.ProjectionQueryResult

data class PdlOrderSummary(val orderId: String = "")

class PdlOrderQueryService(private val store: IEventStore) {
    suspend fun getOrderSummaries(): List<PdlOrderSummary> {
        val result = store.projections.query(
            """
            projection OrderSummary
              from OrderPlaced
            """
        )

        return when (result) {
            is ProjectionQueryResult.Projected -> result.instancesOf(PdlOrderSummary::class)
            is ProjectionQueryResult.Invalid -> emptyList()
        }
    }
}
```
