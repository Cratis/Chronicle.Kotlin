```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

@EventType
class MbCountersUserConnected

@EventType
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
