```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.testing.ReadModelScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class OrderCreated(val orderId: String)

@ReadModel
data class OrderSummary(val orderId: String = "", val total: Double = 0.0)

interface PricingService {
    fun basePrice(): Double
}

@Reducer
class OrderSummaryReducer(private val pricingService: PricingService) {
    fun orderCreated(event: OrderCreated) = OrderSummary(event.orderId, pricingService.basePrice())
}

class InjectingDependenciesTests {

    @Test
    fun `a fake passed into the reducer's constructor is used while folding`() = runBlocking {
        val pricingService = object : PricingService {
            override fun basePrice() = 42.0
        }

        val scenario = ReadModelScenario<OrderSummary>(OrderSummaryReducer(pricingService))
        val instance = scenario.fold("order-1", OrderCreated("order-1"))

        assertEquals(42.0, instance!!.total)
    }
}
```
