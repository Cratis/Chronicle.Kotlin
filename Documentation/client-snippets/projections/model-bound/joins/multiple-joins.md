```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbJoinsMultipleOrderPlaced(val customerId: String)

@EventType
data class MbJoinsMultipleCustomerCreated(val name: String)

@EventType
data class MbJoinsCustomerUpdated(val email: String)

@EventType
data class MbJoinsShippingAddressSet(val address: String)

@ReadModel
@FromEvent(MbJoinsMultipleOrderPlaced::class)
data class MbJoinsEnrichedOrder(
    @SetFrom("customerId", MbJoinsMultipleOrderPlaced::class)
    val customerId: String = "",

    @Join(MbJoinsMultipleCustomerCreated::class, on = "customerId")
    val customerName: String = "",

    @Join(MbJoinsCustomerUpdated::class, on = "customerId")
    val customerEmail: String = "",

    // ShippingAddressSet is raised on the order's own event source, so it joins on the
    // read model's own event source id rather than a separate correlating property.
    @Join(MbJoinsShippingAddressSet::class)
    val shippingAddress: String = ""
)
```
