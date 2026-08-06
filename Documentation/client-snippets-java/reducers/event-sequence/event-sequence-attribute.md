```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.EventSequence;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "reducers-event-sequence-refund-issued")
record ReducersEventSequenceRefundIssued(double amount) {}

@ReadModel
record ReducersEventSequenceRefundSummary(double refunded) {
    ReducersEventSequenceRefundSummary() {
        this(0.0);
    }
}

@Reducer
@EventSequence("outbox")
class ReducersEventSequenceRefundSummaryReducer {
    ReducersEventSequenceRefundSummary issued(
            ReducersEventSequenceRefundIssued event,
            ReducersEventSequenceRefundSummary current) {
        double refunded = current == null ? 0.0 : current.refunded();
        return new ReducersEventSequenceRefundSummary(refunded + event.amount());
    }
}
```
