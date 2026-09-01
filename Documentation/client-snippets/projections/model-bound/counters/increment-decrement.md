```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-counters-user-connected")
class MbCountersUserConnected

@EventType(id = "mb-counters-user-disconnected")
class MbCountersUserDisconnected

@ReadModel
@FromEvent(MbCountersUserConnected::class)
@FromEvent(MbCountersUserDisconnected::class)
data class MbCountersServerStatistics(
    @Increment(MbCountersUserConnected::class)
    @Decrement(MbCountersUserDisconnected::class)
    val activeConnections: Int = 0
)
```
