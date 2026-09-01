```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.Reactor;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterBySourceTypeCustomerRegistered(String emailAddress) {}

@EventSourceType("customer")
@Reactor
class FilterBySourceTypeCustomerWelcomeReactor {
    public void customerRegistered(FilterBySourceTypeCustomerRegistered event, EventContext context) {
        // Only invoked for events appended with eventSourceType: "customer"
    }
}

class EventsFilteringByEventSourceTypeReactor {
    AppendResult register(EventStore store, String eventSourceId, String emailAddress) {
        AppendOptions options = new AppendOptionsBuilder().eventSourceType("customer").build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterBySourceTypeCustomerRegistered(emailAddress), options);
    }
}
```
