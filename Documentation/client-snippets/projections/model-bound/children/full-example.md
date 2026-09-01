```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

// Events
@EventType(id = "mb-children-full-order-created")
data class MbChildrenFullOrderCreated(val customerName: String)

@EventType(id = "mb-children-full-line-item-added")
data class MbChildrenFullLineItemAdded(
    val itemId: String,
    val productName: String,
    val initialQuantity: Int,
    val unitPrice: Double
)

@EventType(id = "mb-children-full-quantity-adjusted")
data class MbChildrenFullQuantityAdjusted(val itemId: String, val newQuantity: Int)

@EventType(id = "mb-children-full-line-item-removed")
data class MbChildrenFullLineItemRemoved(val itemId: String)

// Read Models
@ReadModel
@FromEvent(MbChildrenFullOrderCreated::class)
data class MbChildrenFullOrder(
    @SetFrom("customerName", MbChildrenFullOrderCreated::class)
    val customer: String = "",

    @ChildrenFrom(MbChildrenFullLineItemAdded::class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(MbChildrenFullQuantityAdjusted::class, key = "itemId", identifiedBy = "itemId")
    @RemovedWith(MbChildrenFullLineItemRemoved::class, key = "itemId")
    val lines: List<MbChildrenFullOrderLine> = emptyList()
)

data class MbChildrenFullOrderLine(
    val itemId: String = "",

    @SetFrom("productName", MbChildrenFullLineItemAdded::class)
    val product: String = "",

    @SetFrom("initialQuantity", MbChildrenFullLineItemAdded::class)
    @SetFrom("newQuantity", MbChildrenFullQuantityAdjusted::class)
    val quantity: Int = 0,

    @SetFrom("unitPrice", MbChildrenFullLineItemAdded::class)
    val unitPrice: Double = 0.0
)
```
