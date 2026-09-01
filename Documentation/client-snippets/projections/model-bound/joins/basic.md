```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbJoinsOrderPlaced(val customerId: String, val amount: Double)

@EventType
data class MbJoinsCustomerCreated(val name: String)

@ReadModel
@FromEvent(MbJoinsOrderPlaced::class)
data class MbJoinsOrderSummary(
    @SetFrom("amount", MbJoinsOrderPlaced::class)
    val amount: Double = 0.0,

    @SetFrom("customerId", MbJoinsOrderPlaced::class)
    val customerId: String = "",

    @Join(MbJoinsCustomerCreated::class, on = "customerId", eventPropertyName = "name")
    val customerName: String = ""
)
```
