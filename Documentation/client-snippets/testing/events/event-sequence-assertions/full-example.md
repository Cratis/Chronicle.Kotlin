```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqFullExampleAuthorRegistered(val name: String)

@EventType
data class TestingSeqFullExampleBookAdded(val title: String)

class RegisteringAnAuthorAndAddingABookTests {

    @Test
    fun `both events land in order`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.eventLog.append(authorId, TestingSeqFullExampleAuthorRegistered("Jane Smith"))
        scenario.eventLog.append(authorId, TestingSeqFullExampleBookAdded("Clean Code"))

        assertEquals(1L, scenario.eventLog.getTailSequenceNumber().value)

        val author = scenario.shouldHaveAppended<TestingSeqFullExampleAuthorRegistered>(authorId) { it.name == "Jane Smith" }
        val book = scenario.shouldHaveAppended<TestingSeqFullExampleBookAdded>(authorId) { it.title == "Clean Code" }

        assertEquals("Jane Smith", author.name)
        assertEquals("Clean Code", book.title)
    }
}
```
