```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqByEventSourceAuthorRegistered(val name: String)

class ByEventSourceTests {

    @Test
    fun `an appended event is scoped to its own event source`() = runBlocking {
        val scenario = EventScenario()
        val author1 = "author-1"
        val author2 = "author-2"

        scenario.eventLog.append(author1, TestingSeqByEventSourceAuthorRegistered("Jane Smith"))
        scenario.eventLog.append(author2, TestingSeqByEventSourceAuthorRegistered("John Doe"))

        val first = scenario.shouldHaveAppended<TestingSeqByEventSourceAuthorRegistered>(author1) { it.name == "Jane Smith" }
        val second = scenario.shouldHaveAppended<TestingSeqByEventSourceAuthorRegistered>(author2) { it.name == "John Doe" }

        assertEquals("Jane Smith", first.name)
        assertEquals("John Doe", second.name)
    }
}
```
