```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.testing.ReadModelScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class SomeEvent(val value: String)

@ReadModel
data class MyReadModel(val value: String = "")

@Reducer
class MyReducer {
    fun someEvent(event: SomeEvent) = MyReadModel(event.value)
}

class WhenProjectingEventsTests {

    @Test
    fun `folding an event produces the read model`() = runBlocking {
        val scenario = ReadModelScenario<MyReadModel>(MyReducer())

        val instance = scenario.fold("some-id", SomeEvent("expected value"))

        assertEquals("expected value", instance!!.value)
    }
}
```
