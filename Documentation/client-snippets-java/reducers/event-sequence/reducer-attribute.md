```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record ReducersEventSequencePaymentSettled(double amount) {}

@ReadModel
record ReducersEventSequencePaymentSummary(double settled) {
    ReducersEventSequencePaymentSummary() {
        this(0.0);
    }
}

@Reducer(id = "payment-summary", eventSequence = "outbox")
class ReducersEventSequencePaymentSummaryReducer {
    ReducersEventSequencePaymentSummary settled(
            ReducersEventSequencePaymentSettled event,
            ReducersEventSequencePaymentSummary current) {
        double settled = current == null ? 0.0 : current.settled();
        return new ReducersEventSequencePaymentSummary(settled + event.amount());
    }
}
```
