```kotlin title="Use the read model property name by convention"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ProductRenamedForEveryConvention(val name: String, val version: Int)

@EventType
data class ProductPriceChangedForEveryConvention(val price: Double, val version: Int)

@ReadModel
@FromEvent(ProductRenamedForEveryConvention::class)
@FromEvent(ProductPriceChangedForEveryConvention::class)
data class ProductVersionFromEveryConvention(
    val name: String = "",
    val price: Double = 0.0,

    @FromEvery
    val version: Int = 0
)
```
