```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-set-value-order-placed")
data class MbSetValueOrderPlaced(val customerName: String)

@EventType(id = "mb-set-value-order-canceled")
data class MbSetValueOrderCanceled(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbSetValueOrderPlaced::class)
@FromEvent(MbSetValueOrderCanceled::class)
data class MbSetValueOrder(
    @SetFrom("customerName", MbSetValueOrderPlaced::class)
    val customerName: String = "",

    @SetValue(MbSetValueOrderPlaced::class, value = "active")
    @SetValue(MbSetValueOrderCanceled::class, value = "canceled")
    val status: String = ""
)
```
