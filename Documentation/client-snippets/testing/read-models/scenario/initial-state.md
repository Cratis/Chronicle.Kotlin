```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
class InitialStateItemAdded

@ReadModel
data class CountingReadModel(val count: Int)

@Reducer
class CountingReducer {
    fun itemAdded(event: InitialStateItemAdded, current: CountingReadModel) =
        current.copy(count = current.count + 1)
}

/**
 * [io.cratis.chronicle.testing.ReadModelScenario] has no constructor parameter for a starting
 * baseline, so a non-default initial state is passed directly to the reducer's current-state
 * parameter.
 */
class InitialStateTests {

    @Test
    fun `folding from a non-default baseline continues from it`() {
        val reducer = CountingReducer()
        val initial = CountingReadModel(10)

        val result = reducer.itemAdded(InitialStateItemAdded(), initial)

        assertEquals(11, result.count)
    }
}
```
