```kotlin
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(IndexExplicitAccountOpened::class)
data class IndexExplicitMbAccountInfo(
    @SetFrom("name", IndexExplicitAccountOpened::class)
    val name: String = "",

    @SetFrom("initialBalance", IndexExplicitAccountOpened::class)
    val balance: Double = 0.0
)
```
