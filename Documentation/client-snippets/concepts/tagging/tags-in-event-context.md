```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class TaggingUserAnalytics(val loginCount: Int = 0, val criticalLoginCount: Int = 0)

@Reducer
class TaggingUserAnalyticsReducer {
    fun loggedIn(
        event: TaggingUserLoggedIn,
        current: TaggingUserAnalytics?,
        context: EventContext
    ): TaggingUserAnalytics {
        val analytics = current ?: TaggingUserAnalytics()

        // Tags the event was appended with are available on the context.
        val isCritical = context.tags.contains("critical")

        return analytics.copy(
            loginCount = analytics.loginCount + 1,
            criticalLoginCount = analytics.criticalLoginCount + if (isCritical) 1 else 0
        )
    }
}
```
