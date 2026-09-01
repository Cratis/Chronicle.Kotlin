```kotlin title="Convention-based FromAll attribute"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromAll
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "product-renamed-from-all-convention")
data class ProductRenamedFromAllConvention(val name: String, val version: Int)

@EventType(id = "product-price-changed-from-all-convention")
data class ProductPriceChangedFromAllConvention(val price: Double, val version: Int)

@ReadModel
@FromEvent(ProductRenamedFromAllConvention::class)
@FromEvent(ProductPriceChangedFromAllConvention::class)
data class ProductVersionFromAllConvention(
    val name: String = "",
    val price: Double = 0.0,

    @FromAll
    val version: Int = 0
)
```
