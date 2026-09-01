```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

@EventType
record PassiveReducersTransactionCompleted(double amount) {}

@ReadModel
record PassiveReducersAdHocReport(double totalRevenue, int transactionCount, Instant generatedAt) {
    PassiveReducersAdHocReport() {
        this(0.0, 0, Instant.EPOCH);
    }
}

@Reducer(isActive = false)
class PassiveReducersAdHocReportReducer {
    PassiveReducersAdHocReport completed(PassiveReducersTransactionCompleted event, PassiveReducersAdHocReport current, EventContext context) {
        double revenue = current == null ? 0.0 : current.totalRevenue();
        int count = current == null ? 0 : current.transactionCount();

        return new PassiveReducersAdHocReport(revenue + event.amount(), count + 1, context.getOccurred());
    }
}
```
