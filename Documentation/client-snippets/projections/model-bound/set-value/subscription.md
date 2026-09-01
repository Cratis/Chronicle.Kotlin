```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-set-value-subscription-started")
data class MbSetValueSubscriptionStarted(val placeholder: Boolean = true)

@EventType(id = "mb-set-value-subscription-paused")
data class MbSetValueSubscriptionPaused(val placeholder: Boolean = true)

@EventType(id = "mb-set-value-subscription-canceled")
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
