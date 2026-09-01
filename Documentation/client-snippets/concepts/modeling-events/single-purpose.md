```kotlin
import io.cratis.chronicle.events.EventType

data class ModelingEventsCustomerName(val value: String)
data class ModelingEventsEmail(val value: String)
data class ModelingEventsDeactivationReason(val value: String)
data class ModelingEventsCustomerAddress(val street: String, val city: String)

// One event trying to be everything — consumers must guess what changed
@EventType
data class ModelingEventsCustomerUpdated(
    val name: ModelingEventsCustomerName?,
    val address: ModelingEventsCustomerAddress?,
    val email: ModelingEventsEmail?,
    val deactivated: Boolean?
)

// Distinct facts — each consumer subscribes to exactly what it cares about
@EventType
data class ModelingEventsCustomerRenamed(val name: ModelingEventsCustomerName)

@EventType
data class ModelingEventsCustomerAddressChanged(val address: ModelingEventsCustomerAddress)

@EventType
data class ModelingEventsCustomerDeactivated(val reason: ModelingEventsDeactivationReason)
```
