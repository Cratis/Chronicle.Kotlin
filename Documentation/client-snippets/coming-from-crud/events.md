```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class CrudComparisonCustomerRegistered(val name: String, val address: String)

@EventType
data class CrudComparisonAddressChanged(val address: String)
```
