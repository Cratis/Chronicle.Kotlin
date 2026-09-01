```kotlin
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PassiveReducersExperimentalMetrics(val sampleCount: Int = 0)

// Registered but not run while a metric is still being worked out - the read model is only
// produced on demand until you are ready to make it active.
@Reducer(isActive = false)
class PassiveReducersExperimentalMetricsReducer
```
