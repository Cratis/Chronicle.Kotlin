```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

class DecFromEventSequenceEventSequences {
    // Using a constant instead of a raw string keeps the sequence identifier consistent
    // wherever it is referenced.
    public static final String ORDER_MANAGEMENT = "order-management";
}

@Projection(eventSequence = DecFromEventSequenceEventSequences.ORDER_MANAGEMENT)
class DecFromEventSequenceOrderProjectionWithConstant implements IProjectionFor<DecFromEventSequenceOrder> {
    @Override
    public void define(IProjectionBuilderFor<DecFromEventSequenceOrder> builder) {
        builder.from(DecFromEventSequenceOrderCreated.class);
    }
}
```
