```java
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record ReducersGettingStartedAttributeOrderSummary(String orderId) {
    ReducersGettingStartedAttributeOrderSummary() {
        this("");
    }
}

@Reducer(id = "order-summary")
class ReducersGettingStartedAttributeOrderSummaryReducer {
}
```
