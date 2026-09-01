```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

@Projection(eventSequence = "order-management")
class DecFromEventSequenceOrderProjection implements IProjectionFor<DecFromEventSequenceOrder> {
    @Override
    public void define(IProjectionBuilderFor<DecFromEventSequenceOrder> builder) {
        builder.from(DecFromEventSequenceOrderCreated.class);
        builder.from(DecFromEventSequenceOrderUpdated.class);
        builder.from(DecFromEventSequenceOrderShipped.class);
    }
}
```
