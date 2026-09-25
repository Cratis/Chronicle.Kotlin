```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag({"Notifications", "Customer", "Email"})
@Reactor
class TaggingCustomerNotificationReactor {
    void on(TaggingCustomerRegistered event) { }
}

@EventType
record TaggingCustomerRegistered(String id) {}
```
