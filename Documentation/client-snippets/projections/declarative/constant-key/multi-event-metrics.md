```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecConstantKeyPageViewed(val pageUrl: String)

@EventType
data class DecConstantKeyButtonClicked(val buttonId: String)

@EventType
data class DecConstantKeyFormSubmitted(val formId: String)

data class DecConstantKeyEngagementMetrics(
    val pageViews: Int = 0,
    val buttonClicks: Int = 0,
    val formSubmissions: Int = 0
)

class DecConstantKeyEngagementMetricsProjection : IProjectionFor<DecConstantKeyEngagementMetrics> {
    override fun define(builder: IProjectionBuilderFor<DecConstantKeyEngagementMetrics>) {
        builder
            .from(DecConstantKeyPageViewed::class) {
                it.usingConstantKey("metrics")
                it.count(DecConstantKeyEngagementMetrics::pageViews)
            }
            .from(DecConstantKeyButtonClicked::class) {
                it.usingConstantKey("metrics")
                it.count(DecConstantKeyEngagementMetrics::buttonClicks)
            }
            .from(DecConstantKeyFormSubmitted::class) {
                it.usingConstantKey("metrics")
                it.count(DecConstantKeyEngagementMetrics::formSubmissions)
            }
    }
}
```
