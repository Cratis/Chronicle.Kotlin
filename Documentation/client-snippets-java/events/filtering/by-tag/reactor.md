```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.Reactor;

import java.util.List;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterByTagCustomerRegistered(String emailAddress) {}

@FilterEventsByTag("vip")
@Reactor
class FilterByTagVipWelcomeReactor {
    public void customerRegistered(FilterByTagCustomerRegistered event, EventContext context) {
        // Only receives events appended with the "vip" tag
    }
}

class EventsFilteringByTagReactor {
    AppendResult register(EventStore store, String eventSourceId, String emailAddress) {
        AppendOptions options = new AppendOptionsBuilder().tags(List.of("vip", "onboarding")).build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterByTagCustomerRegistered(emailAddress), options);
    }
}
```
