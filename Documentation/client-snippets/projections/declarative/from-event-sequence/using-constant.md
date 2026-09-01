```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

object DecFromEventSequenceEventSequences {
    const val OrderManagement = "order-management"
}

// Using a constant instead of a raw string keeps the sequence identifier consistent
// wherever it is referenced.
@Projection(eventSequence = DecFromEventSequenceEventSequences.OrderManagement)
class DecFromEventSequenceOrderProjectionWithConstant : IProjectionFor<DecFromEventSequenceOrder> {
    override fun define(builder: IProjectionBuilderFor<DecFromEventSequenceOrder>) {
        builder.from(DecFromEventSequenceOrderCreated::class)
    }
}
```
