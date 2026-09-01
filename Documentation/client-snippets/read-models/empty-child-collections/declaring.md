```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EmptyChildrenOrderPlaced(val customer: String = "")

@EventType
data class EmptyChildrenLineItemAdded(val itemId: String = "", val productName: String = "", val quantity: Int = 0)

data class EmptyChildrenLineItem(val id: String = "", val productName: String = "", val quantity: Int = 0)

// Non-nullable: an order with no line items reads back as an empty collection, so enumerating
// lines never needs a guard.
@ReadModel
@FromEvent(EmptyChildrenOrderPlaced::class)
data class EmptyChildrenOrder(
    val id: String = "",
    val customer: String = "",
    @ChildrenFrom(EmptyChildrenLineItemAdded::class, key = "itemId")
    val lines: List<EmptyChildrenLineItem> = emptyList()
)

// Nullable: "no line items yet" stays distinguishable from "an empty list".
@ReadModel
@FromEvent(EmptyChildrenOrderPlaced::class)
data class EmptyChildrenDraftOrder(
    val id: String = "",
    val customer: String = "",
    @ChildrenFrom(EmptyChildrenLineItemAdded::class, key = "itemId")
    val lines: List<EmptyChildrenLineItem>? = null
)
```
