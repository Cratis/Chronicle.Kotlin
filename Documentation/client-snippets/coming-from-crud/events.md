```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "crud-comparison-customer-registered")
data class CrudComparisonCustomerRegistered(val name: String, val address: String)

@EventType(id = "crud-comparison-address-changed")
data class CrudComparisonAddressChanged(val address: String)
```
