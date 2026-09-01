```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ArchitectureModelBoundItemAdded(val category: String)

@ReadModel
@FromEvent(ArchitectureModelBoundItemAdded::class, key = "category")
data class ArchitectureModelBoundSummary(
    @Count(ArchitectureModelBoundItemAdded::class)
    val count: Int = 0
)
```
