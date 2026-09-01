```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@Projection(eventSequence = "order-management")
class DecFromEventSequenceOrderProjection : IProjectionFor<DecFromEventSequenceOrder> {
    override fun define(builder: IProjectionBuilderFor<DecFromEventSequenceOrder>) {
        builder
            .from(DecFromEventSequenceOrderCreated::class)
            .from(DecFromEventSequenceOrderUpdated::class)
            .from(DecFromEventSequenceOrderShipped::class)
    }
}
```
