```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

// Events
@EventType(id = "mb-constant-key-product-purchased")
data class MbConstantKeyProductPurchased(val productId: String, val amount: Double)

@EventType(id = "mb-constant-key-product-returned")
data class MbConstantKeyProductReturned(val productId: String, val amount: Double)

@EventType(id = "mb-constant-key-page-viewed")
data class MbConstantKeyPageViewed(val pageUrl: String)

// Global read model
@ReadModel
@FromEvent(MbConstantKeyProductPurchased::class)
@FromEvent(MbConstantKeyProductReturned::class)
@FromEvent(MbConstantKeyPageViewed::class)
data class MbConstantKeyStoreMetrics(
    @Count(MbConstantKeyProductPurchased::class, constantKey = "store")
    val totalPurchases: Int = 0,

    @Count(MbConstantKeyProductReturned::class, constantKey = "store")
    val totalReturns: Int = 0,

    @Increment(MbConstantKeyProductPurchased::class, constantKey = "store")
    @Decrement(MbConstantKeyProductReturned::class, constantKey = "store")
    val netTransactions: Int = 0,

    @Count(MbConstantKeyPageViewed::class, constantKey = "store")
    val totalPageViews: Int = 0
)
```
