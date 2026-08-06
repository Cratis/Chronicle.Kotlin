```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "reactors-filtering-shipment-dispatched")
record ReactorsFilteringShipmentDispatched(String trackingNumber) {}

@Reactor
@FilterEventsByTag("priority")
@EventSourceType("Order")
@EventStreamType("Fulfilment")
class ReactorsFilteringShipmentNotifier {
    void dispatched(ReactorsFilteringShipmentDispatched event, EventContext context) {
        // Every filter has to match: the tag, the event source type, and the stream type.
    }
}
```
