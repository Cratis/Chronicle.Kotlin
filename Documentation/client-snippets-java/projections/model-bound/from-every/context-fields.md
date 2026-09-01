```java title="Track audit metadata from every event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEvery;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "auditable-inventory-changed-for-every")
record AuditableInventoryChangedForEvery(String reason) {}

@ReadModel
@FromEvent(eventType = AuditableInventoryChangedForEvery.class)
class AuditableInventoryStatusFromEvery {
    @FromEvery(contextProperty = "occurred")
    public String lastModified = "";

    @FromEvery(contextProperty = "sequenceNumber")
    public String lastEventSequence = "";

    @FromEvery(contextProperty = "correlationId")
    public String lastCorrelationId = "";
}
```
