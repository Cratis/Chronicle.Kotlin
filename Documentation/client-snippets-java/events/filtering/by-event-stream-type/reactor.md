```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.Reactor;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record FilterByStreamTypePaymentCaptured(double amount) {}

@EventStreamType("payments")
@Reactor
class FilterByStreamTypePaymentNotificationsReactor {
    public void paymentCaptured(FilterByStreamTypePaymentCaptured event, EventContext context) {
        // Only handles events appended to the "payments" stream type
    }
}

class EventsFilteringByEventStreamTypeReactor {
    AppendResult capture(EventStore store, String eventSourceId, double amount) {
        AppendOptions options = new AppendOptionsBuilder().eventStreamType("payments").build();
        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new FilterByStreamTypePaymentCaptured(amount), options);
    }
}
```
