```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class ItemAddedToCart(val itemId: String)

@EventType
data class ItemQuantityAdjusted(val itemId: String, val quantity: Int)

class EventScenarioAppendManyTests {

    @Test
    fun `appendMany appends a batch of events in one call`() = runBlocking {
        val scenario = EventScenario()
        val cartId = "cart-1"

        val result = scenario.eventLog.appendMany(
            cartId,
            listOf(
                ItemAddedToCart("item-1"),
                ItemAddedToCart("item-2"),
                ItemQuantityAdjusted("item-1", 3)
            )
        )

        assertTrue(result.all { it.isSuccess })
    }
}
```
