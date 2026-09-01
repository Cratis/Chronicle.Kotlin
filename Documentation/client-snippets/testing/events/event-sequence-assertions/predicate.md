```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.testing.EventScenario
import io.cratis.chronicle.testing.shouldHaveAppended
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@EventType
data class TestingSeqPredicateAuthorRegistered(val name: String)

class PredicateTests {

    @Test
    fun `an appended event satisfies a condition`() = runBlocking {
        val scenario = EventScenario()

        scenario.eventLog.append("author-1", TestingSeqPredicateAuthorRegistered("Jane Smith"))

        scenario.shouldHaveAppended<TestingSeqPredicateAuthorRegistered> { it.name == "Jane Smith" }
    }
}
```
