```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-clearing-shift-planned")
data class MbClearingShiftPlanned(val assignee: String, val hours: Int)

@EventType(id = "mb-clearing-shift-released")
data class MbClearingShiftReleased(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbClearingShiftPlanned::class)
data class MbClearingShift(
    // Nullable, so "nobody is assigned" is a state the property can actually hold.
    @SetValue(MbClearingShiftReleased::class, clear = true)
    val assignee: String? = null,

    // Nullable numeric type, for the same reason: 0 hours is a number of hours, not the absence of one.
    @SetValue(MbClearingShiftReleased::class, clear = true)
    val hours: Int? = null
)
```
