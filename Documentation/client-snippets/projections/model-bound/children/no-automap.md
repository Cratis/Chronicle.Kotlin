```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.NoAutoMap
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbChildrenNoAutoMapOrderPlaced(val orderId: String)

@EventType
data class MbChildrenNoAutoMapLineItemAdded(
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@ReadModel
@FromEvent(MbChildrenNoAutoMapOrderPlaced::class)
data class MbChildrenNoAutoMapOrder(
    @ChildrenFrom(MbChildrenNoAutoMapLineItemAdded::class, key = "itemId")
    val items: List<MbChildrenNoAutoMapLineItem> = emptyList()
)

// Now you must use @SetFrom for each property
@NoAutoMap
data class MbChildrenNoAutoMapLineItem(
    @SetFrom("productName", MbChildrenNoAutoMapLineItemAdded::class)
    val productName: String = "",

    @SetFrom("quantity", MbChildrenNoAutoMapLineItemAdded::class)
    val quantity: Int = 0,

    @SetFrom("price", MbChildrenNoAutoMapLineItemAdded::class)
    val price: Double = 0.0
)
```
