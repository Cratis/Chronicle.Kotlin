```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingScenarioGivenAuthorRegistered(val name: String)

@EventType
data class TestingScenarioGivenBookAdded(val title: String)

class GivenSeedsPreconditionsTests {

    @Test
    fun `preconditions seeded with given are already in the log before the act`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.given(authorId, TestingScenarioGivenAuthorRegistered("John Doe"), TestingScenarioGivenBookAdded("Clean Code"))

        val result = scenario.eventLog.append(authorId, TestingScenarioGivenBookAdded("The Pragmatic Programmer"))
        assertTrue(result.isSuccess)
    }
}
```
