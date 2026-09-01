```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbSetValueSubscriptionStarted(val placeholder: Boolean = true)

@EventType
data class MbSetValueSubscriptionPaused(val placeholder: Boolean = true)

@EventType
data class MbSetValueSubscriptionCanceled(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbSetValueSubscriptionStarted::class)
@FromEvent(MbSetValueSubscriptionPaused::class)
@FromEvent(MbSetValueSubscriptionCanceled::class)
data class MbSetValueSubscription(
    @SetValue(MbSetValueSubscriptionStarted::class, value = "active")
    @SetValue(MbSetValueSubscriptionPaused::class, value = "paused")
    @SetValue(MbSetValueSubscriptionCanceled::class, value = "canceled")
    val state: String = ""
)
```
