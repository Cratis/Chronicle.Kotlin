```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import java.time.Instant;

@EventType(id = "reactor-account-closed")
record ReactorAccountClosed(String accountId) {}

@Reactor
class AuditReactor {
    void accountClosed(ReactorAccountClosed event, EventContext context) {
        writeAudit(event.accountId(), context.getOccurred(), context.getEventSourceId());
    }

    private void writeAudit(String accountId, Instant occurred, String eventSourceId) {}
}
```
