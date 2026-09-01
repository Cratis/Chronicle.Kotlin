```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-event-context-user-action")
record DecEventContextUserAction(String userId, String actionType) {}

class DecEventContextAuditTrailProjection implements IProjectionFor<DecEventContextAuditEntry> {
    @Override
    public void define(IProjectionBuilderFor<DecEventContextAuditEntry> builder) {
        builder
            .autoMap()
            .from(DecEventContextUserAction.class, fb -> {
                fb.set("eventId").toEventContextProperty("sequenceNumber");
                fb.set("occurredAt").toEventContextProperty("occurred");
                fb.set("correlationId").toEventContextProperty("correlationId");
                return null; // Java lambda returning Unit
            });
    }
}
```
