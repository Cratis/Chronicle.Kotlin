```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingAssertionsNoViolationAuthorRegistered(val name: String)

/**
 * [EventScenario] runs in-process with no kernel behind it, so nothing enforces constraints and
 * every append comes back free of constraint violations - there is no kernel there to reject one.
 */
class TestingAssertionsNoViolationTests {

    @Test
    fun `an in-process append never carries a constraint violation`() = runBlocking {
        val scenario = EventScenario()
        val result = scenario.eventLog.append("author-1", TestingAssertionsNoViolationAuthorRegistered("Jane Smith"))

        assertTrue(result.constraintViolations.isEmpty())
    }
}
```
