```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.testing.ReadModelScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class TestingIndexAuthorRegistered(val name: String)

@ReadModel
data class Author(val name: String = "")

@Reducer
class AuthorReducer {
    fun registered(event: TestingIndexAuthorRegistered) = Author(event.name)
}

class WhenProjectingARegisteredAuthorTests {

    @Test
    fun `the author read model carries the registered name`() = runBlocking {
        val scenario = ReadModelScenario<Author>(AuthorReducer())

        val author = scenario.fold("author-1", TestingIndexAuthorRegistered("Jane Austen"))

        assertEquals("Jane Austen", author!!.name)
    }
}
```
