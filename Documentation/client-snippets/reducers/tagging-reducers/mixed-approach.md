```kotlin
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.observation.Tag
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TaggingReducersExecutiveDashboard(val metricCount: Int = 0)

@Reducer
@Tag("Analytics", "Reporting")
@Tag("Executive")
class TaggingReducersExecutiveDashboardReducer
```
