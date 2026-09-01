```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.readModels.ReadModel;

// Events
@EventType(id = "mb-constant-key-product-purchased")
record MbConstantKeyProductPurchased(String productId, double amount) {}

@EventType(id = "mb-constant-key-product-returned")
record MbConstantKeyProductReturned(String productId, double amount) {}

@EventType(id = "mb-constant-key-page-viewed")
record MbConstantKeyPageViewed(String pageUrl) {}

// Global read model
@ReadModel
@FromEvent(eventType = MbConstantKeyProductPurchased.class)
@FromEvent(eventType = MbConstantKeyProductReturned.class)
@FromEvent(eventType = MbConstantKeyPageViewed.class)
class MbConstantKeyStoreMetrics {
    @Count(eventType = MbConstantKeyProductPurchased.class, constantKey = "store")
    public int totalPurchases = 0;

    @Count(eventType = MbConstantKeyProductReturned.class, constantKey = "store")
    public int totalReturns = 0;

    @Increment(eventType = MbConstantKeyProductPurchased.class, constantKey = "store")
    @Decrement(eventType = MbConstantKeyProductReturned.class, constantKey = "store")
    public int netTransactions = 0;

    @Count(eventType = MbConstantKeyPageViewed.class, constantKey = "store")
    public int totalPageViews = 0;
}
```
