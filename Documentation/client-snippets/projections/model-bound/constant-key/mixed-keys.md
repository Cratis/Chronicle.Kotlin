```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
class MbConstantKeyUserRegistered

@EventType
class MbConstantKeyOrderPlacedGlobal

@ReadModel
@FromEvent(MbConstantKeyUserRegistered::class)
@FromEvent(MbConstantKeyOrderPlacedGlobal::class)
data class MbConstantKeyUserDashboard(
    val name: String = "",

    // A per-instance property alongside a constant-keyed one on the same read model
    @Count(MbConstantKeyOrderPlacedGlobal::class, constantKey = "global-stats")
    val platformTotalOrders: Int = 0
)
```
