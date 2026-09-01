```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqAtPositionAuthorRegistered(val name: String)

@EventType
data class TestingSeqAtPositionBookAdded(val title: String)

class AppendedEventAtPositionTests {

    @Test
    fun `an event exists at a specific position in the sequence`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.eventLog.append(authorId, TestingSeqAtPositionAuthorRegistered("Jane Smith"))
        scenario.eventLog.append(authorId, TestingSeqAtPositionBookAdded("Clean Code"))

        val events = scenario.eventLog.events
        val author = chronicleGson.fromJson(events[0].content, TestingSeqAtPositionAuthorRegistered::class.java)
        val book = chronicleGson.fromJson(events[1].content, TestingSeqAtPositionBookAdded::class.java)

        assertEquals("Jane Smith", author.name)
        assertEquals("Clean Code", book.title)
    }
}
```
