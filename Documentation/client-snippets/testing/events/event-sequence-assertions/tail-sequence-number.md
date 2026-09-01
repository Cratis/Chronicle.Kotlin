```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqTailAuthorRegistered(val name: String)

@EventType
data class TestingSeqTailBookAdded(val title: String)

class TailSequenceNumberTests {

    @Test
    fun `the tail sequence number is the position of the last event appended`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.eventLog.append(authorId, TestingSeqTailAuthorRegistered("Jane Smith"))
        scenario.eventLog.append(authorId, TestingSeqTailBookAdded("Clean Code"))

        assertEquals(1L, scenario.eventLog.getTailSequenceNumber().value)
    }
}
```
