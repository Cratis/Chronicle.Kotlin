```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-counters-user-logged-in")
class MbCountersUserLoggedIn

@ReadModel
@FromEvent(MbCountersUserLoggedIn::class)
data class MbCountersUserStatistics(
    @Increment(MbCountersUserLoggedIn::class)
    val loginCount: Int = 0
)
```
