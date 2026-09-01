```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecNotRewindableApiRequestCompleted(val endpoint: String, val statusCode: Int, val durationMilliseconds: Long)

data class DecNotRewindablePerformanceMetric(val timestamp: String = "")

class DecNotRewindablePerformanceMetricProjection : IProjectionFor<DecNotRewindablePerformanceMetric> {
    override fun define(builder: IProjectionBuilderFor<DecNotRewindablePerformanceMetric>) {
        builder
            .notRewindable()
            .autoMap()
            .from(DecNotRewindableApiRequestCompleted::class) {
                it.set(DecNotRewindablePerformanceMetric::timestamp).toEventContextProperty("occurred")
            }
    }
}
```
