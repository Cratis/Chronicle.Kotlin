```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PassiveReducersSwitchableReadModel(val value: Int = 0)

// Was active, now passive
@Reducer(isActive = false)
class PassiveReducersSwitchableReducer {
    fun on(event: PassiveReducersValueRecorded) = PassiveReducersSwitchableReadModel(event.value)
}

@EventType
data class PassiveReducersValueRecorded(val value: Int = 0)
```
