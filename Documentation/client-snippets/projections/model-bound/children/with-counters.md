```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbChildrenCountersItemAddedToCart(
    val itemId: String,
    val productName: String,
    val price: Double,
    val initialQuantity: Int
)

@EventType
data class MbChildrenCountersQuantityIncreased(val itemId: String)

@EventType
data class MbChildrenCountersQuantityDecreased(val itemId: String)

@ReadModel
@FromEvent(MbChildrenCountersItemAddedToCart::class)
data class MbChildrenCountersShoppingCart(
    @ChildrenFrom(MbChildrenCountersItemAddedToCart::class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(MbChildrenCountersQuantityIncreased::class, key = "itemId", identifiedBy = "itemId")
    @ChildrenFrom(MbChildrenCountersQuantityDecreased::class, key = "itemId", identifiedBy = "itemId")
    val items: List<MbChildrenCountersCartItem> = emptyList()
)

// Child type with its own projection attributes
data class MbChildrenCountersCartItem(
    val itemId: String = "",

    @SetFrom("productName", MbChildrenCountersItemAddedToCart::class)
    val productName: String = "",

    @SetFrom("price", MbChildrenCountersItemAddedToCart::class)
    val price: Double = 0.0,

    @SetFrom("initialQuantity", MbChildrenCountersItemAddedToCart::class)
    @Increment(MbChildrenCountersQuantityIncreased::class)
    @Decrement(MbChildrenCountersQuantityDecreased::class)
    val quantity: Int = 0
)
```
