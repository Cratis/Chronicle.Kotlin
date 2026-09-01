```kotlin
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.identity.Identity
import java.time.Instant
import java.util.UUID

// Illustrative subset of io.cratis.chronicle.events.EventContext's real shape
data class EventProcessingEventContextShape(
    val sequenceNumber: Long,
    val eventSourceId: String,
    val eventType: EventTypeDescriptor,
    val occurred: Instant,
    val correlationId: UUID,
    val causedBy: Identity,
    val causation: List<Causation> = emptyList()
)
// ... and more - see EventContext for the full member list
```
