```java title="Business defaults"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

enum InitialValuesOrderStatus {
    Draft,
    Submitted
}

@EventType
record InitialValuesOrderSubmitted(String customerName, double totalAmount) {}

class InitialValuesOrderSummary {
    public String customerName = "";
    public InitialValuesOrderStatus status = InitialValuesOrderStatus.Draft;
    public double totalAmount = 0.0;
    public String notes = "No notes";
}

class InitialValuesOrderSummaryProjection implements IProjectionFor<InitialValuesOrderSummary> {
    @Override
    public void define(IProjectionBuilderFor<InitialValuesOrderSummary> builder) {
        builder.from(InitialValuesOrderSubmitted.class);
    }
}
```
