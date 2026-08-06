```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "reducers-event-sequence-payment-settled")
data class ReducersEventSequencePaymentSettled(val amount: Double)

@ReadModel
data class ReducersEventSequencePaymentSummary(val settled: Double = 0.0)

@Reducer(id = "payment-summary", eventSequence = "outbox")
class ReducersEventSequencePaymentSummaryReducer {
    fun settled(
        event: ReducersEventSequencePaymentSettled,
        current: ReducersEventSequencePaymentSummary?
    ): ReducersEventSequencePaymentSummary =
        ReducersEventSequencePaymentSummary((current?.settled ?: 0.0) + event.amount)
}
```
