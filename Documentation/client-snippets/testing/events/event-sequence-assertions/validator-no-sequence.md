```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqValidatorNoSeqAuthorRegistered(val name: String)

class ValidatorNoSequenceTests {

    @Test
    fun `the first matching event anywhere in the sequence carries the expected content`() = runBlocking {
        val scenario = EventScenario()

        scenario.eventLog.append("author-1", TestingSeqValidatorNoSeqAuthorRegistered("Jane Smith"))

        val author = scenario.shouldHaveAppended<TestingSeqValidatorNoSeqAuthorRegistered> { it.name == "Jane Smith" }
        assertEquals("Jane Smith", author.name)
    }
}
```
