```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-counters-item-created")
data class MbCountersItemCreated(val name: String, val initialQuantity: Int)

@EventType(id = "mb-counters-item-restocked")
class MbCountersItemRestocked

@EventType(id = "mb-counters-item-sold")
class MbCountersItemSold

@ReadModel
@FromEvent(MbCountersItemCreated::class)
@FromEvent(MbCountersItemRestocked::class)
@FromEvent(MbCountersItemSold::class)
data class MbCountersInventoryItem(
    @SetFrom("name", MbCountersItemCreated::class)
    val name: String = "",

    @SetFrom("initialQuantity", MbCountersItemCreated::class)
    @Increment(MbCountersItemRestocked::class)
    @Decrement(MbCountersItemSold::class)
    val quantity: Int = 0,

    @Count(MbCountersItemRestocked::class)
    val restockCount: Int = 0,

    @Count(MbCountersItemSold::class)
    val salesCount: Int = 0
)
```
