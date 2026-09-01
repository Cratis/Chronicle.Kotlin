```kotlin
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PassiveReducersSwitchableReadModel(val value: Int = 0)

// Was active, now passive
@Reducer(isActive = false)
class PassiveReducersSwitchableReducer
```
