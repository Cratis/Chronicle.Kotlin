```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.EventSourceType;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "reactors-filtering-customer-registered")
record ReactorsFilteringCustomerRegistered(String emailAddress) {}

class ReactorsFilteringCustomerService {
    private final IEventLog eventLog;

    ReactorsFilteringCustomerService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void register(String eventSourceId, String emailAddress) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReactorsFilteringCustomerRegistered(emailAddress),
            new AppendOptionsBuilder().eventSourceType("customer").build());
    }
}

@Reactor
@EventSourceType("customer")
class ReactorsFilteringCustomerWelcomeReactor {
    void registered(ReactorsFilteringCustomerRegistered event, EventContext context) {
    }
}
```
