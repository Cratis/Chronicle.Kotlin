```kotlin
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.observation.Tag
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TaggingReducersComplianceReport(val status: String = "")

@Reducer
@Tag("Analytics")
@Tag("Compliance")
@Tag("Auditing")
class TaggingReducersComplianceReportReducer
```
