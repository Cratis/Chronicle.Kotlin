```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.testing.ReadModelScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class ReducerExampleOrderCreated(val orderId: String)

@EventType
data class ReducerExampleItemAdded(val price: Double)

@ReadModel
data class ReducerExampleOrderSummary(val orderId: String = "", val total: Double = 0.0)

@Reducer
class ReducerExampleOrderSummaryReducer {
    fun orderCreated(event: ReducerExampleOrderCreated) = ReducerExampleOrderSummary(event.orderId, 0.0)

    fun itemAdded(event: ReducerExampleItemAdded, current: ReducerExampleOrderSummary?): ReducerExampleOrderSummary {
        val existing = current ?: ReducerExampleOrderSummary()
        return existing.copy(total = existing.total + event.price)
    }
}

class ReducerExampleTests {

    @Test
    fun `each event folds into the running total`() = runBlocking {
        val scenario = ReadModelScenario<ReducerExampleOrderSummary>(ReducerExampleOrderSummaryReducer())

        val instance = scenario.fold(
            "order-1",
            ReducerExampleOrderCreated("order-1"),
            ReducerExampleItemAdded(9.99),
            ReducerExampleItemAdded(4.50)
        )

        assertEquals(14.49, instance!!.total, 0.001)
    }
}
```
