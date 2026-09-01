```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@EventType
data class DecFromEventSequencePackageCreated(val packageId: String)

@EventType
data class DecFromEventSequencePackageShipped(val packageId: String, val shippedAt: String)

@EventType
data class DecFromEventSequencePackageDelivered(val packageId: String, val deliveredAt: String)

data class DecFromEventSequenceShipping(
    val packageId: String = "",
    val shippedAt: String? = null,
    val deliveredAt: String? = null
)

// Projection for order management events
@Projection(eventSequence = "order-management")
class DecFromEventSequenceMultiOrderProjection : IProjectionFor<DecFromEventSequenceOrder> {
    override fun define(builder: IProjectionBuilderFor<DecFromEventSequenceOrder>) {
        builder
            .from(DecFromEventSequenceOrderCreated::class)
            .from(DecFromEventSequenceOrderUpdated::class)
            .from(DecFromEventSequenceOrderShipped::class)
    }
}

// Projection for shipping events from a different sequence
@Projection(eventSequence = "shipping-management")
class DecFromEventSequenceShippingProjection : IProjectionFor<DecFromEventSequenceShipping> {
    override fun define(builder: IProjectionBuilderFor<DecFromEventSequenceShipping>) {
        builder
            .from(DecFromEventSequencePackageCreated::class)
            .from(DecFromEventSequencePackageShipped::class)
            .from(DecFromEventSequencePackageDelivered::class)
    }
}
```
