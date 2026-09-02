```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.math.BigDecimal;

@EventType
record DecFunctionsTransaction(BigDecimal amount) {}

class DecFunctionsTransactionSummary {
    public int transactionCount = 0;
    public BigDecimal totalAmount = BigDecimal.ZERO;
    public int processedEvents = 0;
}

class DecFunctionsTransactionSummaryProjection implements IProjectionFor<DecFunctionsTransactionSummary> {
    @Override
    public void define(IProjectionBuilderFor<DecFunctionsTransactionSummary> builder) {
        builder
            .from(DecFunctionsTransaction.class, fb -> {
                fb.count("transactionCount");
                fb.add("totalAmount").with("amount");
                fb.increment("processedEvents");
            });
    }
}
```
