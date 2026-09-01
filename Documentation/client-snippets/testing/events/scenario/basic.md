```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingScenarioBasicAuthorRegistered(val name: String)

class EventScenarioBasicTests {

    @Test
    fun `appending an event through the scenario succeeds`() = runBlocking {
        val scenario = EventScenario()
        val result = scenario.eventLog.append("author-1", TestingScenarioBasicAuthorRegistered("John Doe"))

        assertTrue(result.isSuccess)
    }
}
```
