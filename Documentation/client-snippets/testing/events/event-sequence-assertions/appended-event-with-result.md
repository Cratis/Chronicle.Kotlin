```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendedEventWithResult
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqWithResultAuthorRegistered(val name: String)

class AppendedEventWithResultTests {

    @Test
    fun `pairing an appended event with its append result`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"
        val event = TestingSeqWithResultAuthorRegistered("Jane Smith")

        val result = scenario.eventLog.append(authorId, event)
        val context = scenario.eventLog.events.last().context
        val collected = AppendedEventWithResult(context, event, result)

        assertTrue(collected.result.isSuccess)
        assertEquals("Jane Smith", (collected.event as TestingSeqWithResultAuthorRegistered).name)
        assertEquals(authorId, collected.context.eventSourceId)
    }
}
```
