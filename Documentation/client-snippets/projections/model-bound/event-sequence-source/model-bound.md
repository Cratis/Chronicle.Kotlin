```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.EventSequence
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbEventSeqOrderPlaced(val amount: Double)

@ReadModel
@EventSequence("custom-sequence")
@FromEvent(MbEventSeqOrderPlaced::class)
data class MbEventSeqOrderSummary(
    @SetFrom("amount", MbEventSeqOrderPlaced::class)
    val totalAmount: Double = 0.0
)
```
