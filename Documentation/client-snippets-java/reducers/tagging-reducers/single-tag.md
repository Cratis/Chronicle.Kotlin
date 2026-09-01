```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record TaggingReducersOrderPlaced(double totalAmount) {}

@ReadModel
record TaggingReducersOrderAnalytics(int orderCount, double totalAmount) {
    TaggingReducersOrderAnalytics() {
        this(0, 0.0);
    }
}

@Reducer
@Tag("Analytics")
class TaggingReducersOrderAnalyticsReducer {
    TaggingReducersOrderAnalytics placed(TaggingReducersOrderPlaced event, TaggingReducersOrderAnalytics current, EventContext context) {
        int count = current == null ? 0 : current.orderCount();
        double total = current == null ? 0.0 : current.totalAmount();
        return new TaggingReducersOrderAnalytics(count + 1, total + event.totalAmount());
    }
}
```
