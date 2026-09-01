```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecNotRewindablePaymentProcessed(String paymentId, double amount) {}

class DecNotRewindableLedgerEntry {
    public String recordedAt = "";
}

class DecNotRewindableTransactionLedgerProjection implements IProjectionFor<DecNotRewindableLedgerEntry> {
    @Override
    public void define(IProjectionBuilderFor<DecNotRewindableLedgerEntry> builder) {
        builder
            .notRewindable()
            .fromEvery(feb -> {
                feb.set("recordedAt").toEventContextProperty("occurred");
                return null; // Java lambda returning Unit
            })
            .from(DecNotRewindablePaymentProcessed.class);
    }
}
```
