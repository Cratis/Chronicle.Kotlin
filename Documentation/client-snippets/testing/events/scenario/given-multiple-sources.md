```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@EventType
data class TestingScenarioGivenMultiAuthorRegistered(val name: String)

class GivenMultipleSourcesTests {

    @Test
    fun `given seeds events for different event sources independently`() = runBlocking {
        val scenario = EventScenario()

        scenario.given("author-1", TestingScenarioGivenMultiAuthorRegistered("Jane Smith"))
        scenario.given("author-2", TestingScenarioGivenMultiAuthorRegistered("John Doe"))
    }
}
```
