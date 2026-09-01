```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.EventSequence
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ReducersEventSequenceRefundIssued(val amount: Double)

@ReadModel
data class ReducersEventSequenceRefundSummary(val refunded: Double = 0.0)

@Reducer
@EventSequence("outbox")
class ReducersEventSequenceRefundSummaryReducer {
    fun issued(
        event: ReducersEventSequenceRefundIssued,
        current: ReducersEventSequenceRefundSummary?
    ): ReducersEventSequenceRefundSummary =
        ReducersEventSequenceRefundSummary((current?.refunded ?: 0.0) + event.amount)
}
```
