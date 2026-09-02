```java title="Composite key projection"
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class CompositeOrderProjection implements IProjectionFor<CompositeOrder> {
    @Override
    public void define(IProjectionBuilderFor<CompositeOrder> builder) {
        builder.from(CompositeOrderCreated.class, it -> {
            it.usingCompositeKey(key -> {
                key.property("customerId", "customerId").property("orderNumber", "orderNumber");
            });
        });
        builder.from(CompositeOrderShipped.class, it -> {
            it.usingCompositeKey(key -> {
                key.property("customerId", "customerId").property("orderNumber", "orderNumber");
            });
        });
    }
}
```
