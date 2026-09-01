```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "event-processing-hourly-metric-recorded")
data class EventProcessingHourlyMetricRecorded(val value: Double)

@ReadModel
data class EventProcessingHourlyMetrics(val metricsByHour: Map<Int, Double> = emptyMap())

@Reducer
class EventProcessingHourlyMetricsReducer {
    fun recorded(event: EventProcessingHourlyMetricRecorded, current: EventProcessingHourlyMetrics?, context: EventContext): EventProcessingHourlyMetrics {
        val hour = context.occurred.atZone(java.time.ZoneOffset.UTC).hour
        val metricsByHour = (current?.metricsByHour ?: emptyMap()) +
            (hour to (current?.metricsByHour?.get(hour) ?: 0.0) + event.value)

        return EventProcessingHourlyMetrics(metricsByHour)
    }
}
```
