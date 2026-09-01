```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqByTypeAuthorRegistered(val name: String)

@EventType
data class TestingSeqByTypeBookAdded(val title: String)

class AppendedEventByTypeTests {

    @Test
    fun `at least one event of a type was appended somewhere in the sequence`() = runBlocking {
        val scenario = EventScenario()
        val authorId = "author-1"

        scenario.eventLog.append(authorId, TestingSeqByTypeAuthorRegistered("Jane Smith"))
        scenario.eventLog.append(authorId, TestingSeqByTypeBookAdded("Clean Code"))

        scenario.shouldHaveAppended<TestingSeqByTypeAuthorRegistered>()
        scenario.shouldHaveAppended<TestingSeqByTypeBookAdded>()
    }
}
```
