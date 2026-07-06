```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(IndexAutoMapAccountOpened::class)
data class IndexAutoMapMbAccountInfo(
    val name: String = "",     // Automatically mapped from IndexAutoMapAccountOpened.name
    val balance: Double = 0.0  // Automatically mapped from IndexAutoMapAccountOpened.balance
)
```
