```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbEventSeqLocalEvent(val data: String)

// No @EventSequence needed — an observer with no event sequence specified observes the event log.
@ReadModel
@FromEvent(MbEventSeqLocalEvent::class)
data class MbEventSeqLocalSnapshot(
    @SetFrom("data", MbEventSeqLocalEvent::class)
    val data: String = ""
)
```
