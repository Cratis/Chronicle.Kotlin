```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;

@EventType
record ReactorsFilteringByTagOrderPlaced(double totalAmount) {}

class ReactorsFilteringByTagOrderService {
    private final IEventLog eventLog;

    ReactorsFilteringByTagOrderService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void placePriorityOrder(String eventSourceId, double totalAmount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReactorsFilteringByTagOrderPlaced(totalAmount),
            new AppendOptionsBuilder().tag("priority").build());
    }
}

@Reactor
@FilterEventsByTag("priority")
class ReactorsFilteringPriorityOrderNotifier {
    void placed(ReactorsFilteringByTagOrderPlaced event, EventContext context) {
    }
}
```
