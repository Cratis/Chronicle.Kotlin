```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.observation.Tag
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TaggingReducersSalesReport(val totalSales: Double = 0.0)

// @Tag takes any number of tags in a single attribute
@Reducer
@Tag("Analytics", "Reporting", "Dashboard")
class TaggingReducersSalesReportReducer {
    fun on(event: TaggingReducersSaleRecorded) = TaggingReducersSalesReport(event.totalSales)
}

@EventType
data class TaggingReducersSaleRecorded(val totalSales: Double = 0.0)
```
