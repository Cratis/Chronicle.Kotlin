```kotlin title="Composite key projection"
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class CompositeOrderProjection : IProjectionFor<CompositeOrder> {
    override fun define(builder: IProjectionBuilderFor<CompositeOrder>) {
        builder
            .from(CompositeOrderCreated::class) {
                it.usingCompositeKey { key ->
                    key.property("customerId", "customerId")
                        .property("orderNumber", "orderNumber")
                }
            }
            .from(CompositeOrderShipped::class) {
                it.usingCompositeKey { key ->
                    key.property("customerId", "customerId")
                        .property("orderNumber", "orderNumber")
                }
            }
    }
}
```
