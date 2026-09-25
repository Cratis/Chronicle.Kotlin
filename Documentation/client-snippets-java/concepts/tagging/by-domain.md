```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag("Sales")
@Tag("Inventory")
@Tag("Customer")
@Tag("Shipping")
@Reactor
class TaggingByDomainExample {
    void on(TaggingByDomainOrderPlaced event) { }
}

@EventType
record TaggingByDomainOrderPlaced(String id) {}
```
