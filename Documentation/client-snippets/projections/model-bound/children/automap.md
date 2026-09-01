```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-children-automap-line-item-added")
data class MbChildrenAutoMapLineItemAdded(
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@ReadModel
@FromEvent(MbChildrenAutoMapLineItemAdded::class)
data class MbChildrenAutoMapOrder(
    @ChildrenFrom(MbChildrenAutoMapLineItemAdded::class, key = "itemId", identifiedBy = "itemId")
    val items: List<MbChildrenAutoMapLineItem> = emptyList()
)

data class MbChildrenAutoMapLineItem(
    val itemId: String = "",
    val productName: String = "",  // Automatically mapped from MbChildrenAutoMapLineItemAdded.productName
    val quantity: Int = 0,          // Automatically mapped from MbChildrenAutoMapLineItemAdded.quantity
    val price: Double = 0.0         // Automatically mapped from MbChildrenAutoMapLineItemAdded.price
)
```
