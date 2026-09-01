```kotlin title="Product promotion projection"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.time.Instant

@EventType
data class ProductListedWithNestedPromotion(val name: String, val basePrice: Double)

@EventType
data class PromotionAppliedWithNestedPromotion(
    val label: String,
    val discountPercent: Int,
    val validUntil: Instant
)

@EventType
data class PromotionRemovedWithNestedPromotion(val placeholder: Boolean = true)

data class ProductWithNestedPromotion(
    val name: String = "",
    val basePrice: Double = 0.0,
    val promotion: PromotionForNestedProduct? = null
)

data class PromotionForNestedProduct(
    val label: String = "",
    val discountPercent: Int = 0,
    val validUntil: Instant = Instant.EPOCH
)

class ProductProjectionWithNestedPromotion : IProjectionFor<ProductWithNestedPromotion> {
    override fun define(builder: IProjectionBuilderFor<ProductWithNestedPromotion>) {
        builder
            .from(ProductListedWithNestedPromotion::class)
            .nested(ProductWithNestedPromotion::promotion, PromotionForNestedProduct::class) { promotion ->
                promotion
                    .from(PromotionAppliedWithNestedPromotion::class)
                    .clearWith(PromotionRemovedWithNestedPromotion::class)
            }
    }
}
```
