```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "reactors-filtering-multi-tag-order-placed")
record ReactorsFilteringMultiTagOrderPlaced(double totalAmount) {}

@Reactor
@FilterEventsByTag("priority")
@FilterEventsByTag("express")
class ReactorsFilteringFastTrackOrderNotifier {
    void placed(ReactorsFilteringMultiTagOrderPlaced event, EventContext context) {
        // Only events appended with both tags reach this handler.
    }
}
```
