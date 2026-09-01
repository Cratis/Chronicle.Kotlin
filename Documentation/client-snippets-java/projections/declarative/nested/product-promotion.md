```java title="Product promotion projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.time.Instant;

@EventType
record ProductListedWithNestedPromotion(String name, double basePrice) {}

@EventType
record PromotionAppliedWithNestedPromotion(String label, int discountPercent, Instant validUntil) {}

@EventType
record PromotionRemovedWithNestedPromotion() {}

record ProductWithNestedPromotion(String name, double basePrice, PromotionForNestedProduct promotion) {}

record PromotionForNestedProduct(String label, int discountPercent, Instant validUntil) {}

class ProductProjectionWithNestedPromotion implements IProjectionFor<ProductWithNestedPromotion> {
    @Override
    public void define(IProjectionBuilderFor<ProductWithNestedPromotion> builder) {
        builder
            .from(ProductListedWithNestedPromotion.class)
            .nested("promotion", PromotionForNestedProduct.class, promotion -> {
                promotion
                    .from(PromotionAppliedWithNestedPromotion.class)
                    .clearWith(PromotionRemovedWithNestedPromotion.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
