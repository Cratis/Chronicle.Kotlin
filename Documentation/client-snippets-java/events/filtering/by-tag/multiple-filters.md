```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;

@EventType
record FilterByTagMultiCustomerRegistered(String emailAddress) {}

@FilterEventsByTag("vip")
@FilterEventsByTag("priority")
@Reactor
class FilterByTagMultiPriorityNotificationsReactor {
    public void customerRegistered(FilterByTagMultiCustomerRegistered event) {
        // Dispatched when the appended event carries either the "vip" or "priority" tag
    }
}
```
