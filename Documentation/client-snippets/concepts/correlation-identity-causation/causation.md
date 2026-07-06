```kotlin
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager

class CorrelationIdentityCausationCausation {
    fun recordPlaceOrder(orderId: String) {
        causationManager.add(CausationType("MyApp.Commands.PlaceOrder"), mapOf("orderId" to orderId))
    }
}
```
