```kotlin title="Combine FromAll with event-specific mappings"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "order-created-declarative-all")
data class OrderCreatedDeclarativeAll(val orderNumber: String)

@EventType(id = "order-shipped-declarative-all")
data class OrderShippedDeclarativeAll(val trackingNumber: String)

data class OrderDeclarativeAll(
    val orderNumber: String = "",
    val status: String = "",
    val lastModified: String = ""
)

class OrderDeclarativeAllProjection : IProjectionFor<OrderDeclarativeAll> {
    override fun define(builder: IProjectionBuilderFor<OrderDeclarativeAll>) {
        builder
            .fromAll { it.set(OrderDeclarativeAll::lastModified).toEventContextProperty("occurred") }
            .from(OrderCreatedDeclarativeAll::class) {
                it.set(OrderDeclarativeAll::status).to { "Placed" }
            }
            .from(OrderShippedDeclarativeAll::class) {
                it.set(OrderDeclarativeAll::status).to { "Shipped" }
            }
    }
}
```
