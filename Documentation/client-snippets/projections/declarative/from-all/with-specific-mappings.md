```kotlin title="Combine FromAll with event-specific mappings"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class OrderCreatedDeclarativeAll(val orderNumber: String)

@EventType
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
                // status is not set: the JVM fluent builder in 6.4.0 cannot set a constant value.
            }
            .from(OrderShippedDeclarativeAll::class) {
                // status is not set: the JVM fluent builder in 6.4.0 cannot set a constant value.
            }
    }
}
```
