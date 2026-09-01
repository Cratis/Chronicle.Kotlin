```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.testing.EventScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqValidatorAuthorRegistered(val name: String)

class ValidatorTests {

    @Test
    fun `the event at a known position carries the expected content`() = runBlocking {
        val scenario = EventScenario()

        scenario.eventLog.append("author-1", TestingSeqValidatorAuthorRegistered("Jane Smith"))

        val author = chronicleGson.fromJson(scenario.eventLog.events[0].content, TestingSeqValidatorAuthorRegistered::class.java)
        assertEquals("Jane Smith", author.name)
    }
}
```
