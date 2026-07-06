```kotlin title="Matching nested structures and collections"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

data class ConventionAddress(
    val street: String = "",
    val city: String = "",
    val postalCode: String = ""
)

data class ConventionLineItem(
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 0
)

@EventType(id = "convention-customer-registered")
data class ConventionCustomerRegistered(
    val firstName: String,
    val lastName: String,
    val billingAddress: ConventionAddress,
    val shippingAddress: ConventionAddress
)

@EventType(id = "convention-order-created")
data class ConventionOrderCreated(
    val customerEmail: String,
    val items: List<ConventionLineItem>,
    val tags: List<String>
)

@ReadModel
@FromEvent(ConventionCustomerRegistered::class)
data class ConventionCustomer(
    val firstName: String = "",
    val lastName: String = "",
    val billingAddress: ConventionAddress = ConventionAddress(),
    val shippingAddress: ConventionAddress = ConventionAddress()
)

@ReadModel
@FromEvent(ConventionOrderCreated::class)
data class ConventionOrder(
    val customerEmail: String = "",
    val items: List<ConventionLineItem> = emptyList(),
    val tags: List<String> = emptyList()
)
```
