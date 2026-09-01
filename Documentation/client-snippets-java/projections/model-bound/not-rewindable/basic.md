```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.NotRewindable;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbNotRewindableAuditEvent(String message, String occurredAt) {}

@ReadModel
@FromEvent(eventType = MbNotRewindableAuditEvent.class)
@NotRewindable
class MbNotRewindableAuditLog {
    @SetFrom(propertyPath = "message", eventType = MbNotRewindableAuditEvent.class)
    public String message = "";

    @SetFrom(propertyPath = "occurredAt", eventType = MbNotRewindableAuditEvent.class)
    public String timestamp = "";
}
```
