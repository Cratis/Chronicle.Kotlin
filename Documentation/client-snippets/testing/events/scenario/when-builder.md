```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingScenarioWhenBuilderAuthorRegistered(val name: String)

/**
 * Kotlin has no separate `When` builder - the act under test is simply the direct call to
 * [io.cratis.chronicle.eventSequences.IEventSequence.append], and its return value is the "then".
 */
class EventScenarioActPhaseTests {

    @Test
    fun `the act appends the event under test and returns its result`() = runBlocking {
        val scenario = EventScenario()
        val existingAuthorId = "author-1"
        val newAuthorId = "author-2"

        // Given: an author is already registered.
        scenario.given(existingAuthorId, TestingScenarioWhenBuilderAuthorRegistered("John Doe"))

        // When: register a different author under a new event source - the act returns the result.
        val result = scenario.eventLog.append(newAuthorId, TestingScenarioWhenBuilderAuthorRegistered("John Doe"))

        // Then: assert on the returned result.
        assertTrue(result.isSuccess)
    }
}
```
