```java
import io.cratis.chronicle.readModels.ReadModel;
import java.time.Instant;

@ReadModel
record ReducersGettingStartedOrderSummary(
    String orderId,
    double totalAmount,
    int itemCount,
    Instant lastUpdated) {

    ReducersGettingStartedOrderSummary() {
        this("", 0.0, 0, Instant.EPOCH);
    }
}
```
