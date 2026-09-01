```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-passive-snapshot-created")
data class MbPassiveSnapshotCreated(val data: String)

@Passive
@ReadModel
@FromEvent(MbPassiveSnapshotCreated::class)
data class MbPassiveHistoricalSnapshot(
    @SetFrom("data", MbPassiveSnapshotCreated::class)
    val data: String = ""
)
```
