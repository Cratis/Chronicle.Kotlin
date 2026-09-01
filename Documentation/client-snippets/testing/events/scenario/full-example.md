```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingScenarioFullExampleAuthorRegistered(val name: String)

@EventType
data class TestingScenarioFullExampleBookAdded(val title: String)

class WhenAddingABookToAnAuthorTests {

    @Test
    fun `the book is appended after the author is registered`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.given(authorId, TestingScenarioFullExampleAuthorRegistered("Jane Smith"))

        val result = scenario.eventLog.append(authorId, TestingScenarioFullExampleBookAdded("Clean Code"))

        assertTrue(result.isSuccess)
        scenario.shouldHaveAppended<TestingScenarioFullExampleBookAdded>(authorId) { it.title == "Clean Code" }
    }
}
```
