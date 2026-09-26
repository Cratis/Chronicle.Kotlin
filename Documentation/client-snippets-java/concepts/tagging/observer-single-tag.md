```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag("Notifications")
@Reactor
class TaggingOrderConfirmationReactor {
    void on(TaggingOrderConfirmed event) { }
}

@EventType
record TaggingOrderConfirmed(String id) {}
```
