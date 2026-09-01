```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.observation.EventStreamType;
import io.cratis.chronicle.observation.Reactor;

@EventType
record ReactorsFilteringPaymentCaptured(double amount) {}

class ReactorsFilteringPaymentsService {
    private final IEventLog eventLog;

    ReactorsFilteringPaymentsService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void capture(String eventSourceId, double amount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReactorsFilteringPaymentCaptured(amount),
            new AppendOptionsBuilder().eventStreamType("payments").build());
    }
}

@Reactor
@EventStreamType("payments")
class ReactorsFilteringPaymentReceivedNotifier {
    void captured(ReactorsFilteringPaymentCaptured event, EventContext context) {
    }
}
```
