```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbPassiveSnapshotCreated(val data: String)

@Passive
@ReadModel
@FromEvent(MbPassiveSnapshotCreated::class)
data class MbPassiveHistoricalSnapshot(
    @SetFrom("data", MbPassiveSnapshotCreated::class)
    val data: String = ""
)
```
