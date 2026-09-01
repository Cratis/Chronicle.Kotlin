```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-children-removal-property-line-item-added")
data class MbChildrenRemovalPropertyLineItemAdded(val itemId: String, val description: String)

@EventType(id = "mb-children-removal-property-line-item-removed")
data class MbChildrenRemovalPropertyLineItemRemoved(val itemId: String)

@ReadModel
@FromEvent(MbChildrenRemovalPropertyLineItemAdded::class)
data class MbChildrenRemovalPropertyOrder(
    @ChildrenFrom(MbChildrenRemovalPropertyLineItemAdded::class, key = "itemId", identifiedBy = "itemId")
    @RemovedWith(MbChildrenRemovalPropertyLineItemRemoved::class, key = "itemId")
    val lines: List<MbChildrenRemovalPropertyOrderLine> = emptyList()
)

data class MbChildrenRemovalPropertyOrderLine(
    val itemId: String = "",

    @SetFrom("description", MbChildrenRemovalPropertyLineItemAdded::class)
    val description: String = ""
)
```
