```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-children-line-item-added")
data class MbChildrenLineItemAdded(
    val itemId: String,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@ReadModel
@FromEvent(MbChildrenLineItemAdded::class)
data class MbChildrenOrder(
    @ChildrenFrom(MbChildrenLineItemAdded::class, key = "itemId", identifiedBy = "itemId")
    val items: List<MbChildrenLineItem> = emptyList()
)

data class MbChildrenLineItem(
    val itemId: String = "",  // Chronicle automatically discovers this as the key, via identifiedBy
    val productName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)
```
