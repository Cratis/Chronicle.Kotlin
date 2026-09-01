```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

@EventType
record DecFromEventSequencePackageCreated(String packageId) {}

@EventType
record DecFromEventSequencePackageShipped(String packageId, String shippedAt) {}

@EventType
record DecFromEventSequencePackageDelivered(String packageId, String deliveredAt) {}

class DecFromEventSequenceShipping {
    public String packageId = "";
    public String shippedAt = null;
    public String deliveredAt = null;
}

// Projection for order management events
@Projection(eventSequence = "order-management")
class DecFromEventSequenceMultiOrderProjection implements IProjectionFor<DecFromEventSequenceOrder> {
    @Override
    public void define(IProjectionBuilderFor<DecFromEventSequenceOrder> builder) {
        builder.from(DecFromEventSequenceOrderCreated.class);
        builder.from(DecFromEventSequenceOrderUpdated.class);
        builder.from(DecFromEventSequenceOrderShipped.class);
    }
}

// Projection for shipping events from a different sequence
@Projection(eventSequence = "shipping-management")
class DecFromEventSequenceShippingProjection implements IProjectionFor<DecFromEventSequenceShipping> {
    @Override
    public void define(IProjectionBuilderFor<DecFromEventSequenceShipping> builder) {
        builder.from(DecFromEventSequencePackageCreated.class);
        builder.from(DecFromEventSequencePackageShipped.class);
        builder.from(DecFromEventSequencePackageDelivered.class);
    }
}
```
