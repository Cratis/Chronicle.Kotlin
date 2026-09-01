```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingAssertionsAuthorRegistered(val name: String)

class TestingAssertionsHappyPathTests {

    @Test
    fun `appending an event succeeds`() = runBlocking {
        val scenario = EventScenario()
        val result = scenario.eventLog.append("author-1", TestingAssertionsAuthorRegistered("Jane Smith"))

        assertTrue(result.isSuccess)
    }
}
```
