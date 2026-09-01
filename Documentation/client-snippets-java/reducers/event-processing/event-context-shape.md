```java
import io.cratis.chronicle.auditing.Causation;
import io.cratis.chronicle.events.EventTypeDescriptor;
import io.cratis.chronicle.identity.Identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Illustrative subset of io.cratis.chronicle.events.EventContext's real shape
record EventProcessingEventContextShape(
    long sequenceNumber,
    String eventSourceId,
    EventTypeDescriptor eventType,
    Instant occurred,
    UUID correlationId,
    Identity causedBy,
    List<Causation> causation) {}
// ... and more - see EventContext for the full member list
```
