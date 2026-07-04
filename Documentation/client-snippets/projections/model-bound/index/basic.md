```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "index-account-opened")
data class IndexAccountOpened(
    val name: String,
    val initialBalance: Double
)

@ReadModel
@FromEvent(IndexAccountOpened::class)
data class IndexAccountInfo(
    val name: String = "",

    @SetFrom("initialBalance")
    val balance: Double = 0.0
)
```
