```kotlin
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationCausation {
    fun recordPlaceOrder(orderId: String) {
        causationManager.add(CausationType("MyApp.Commands.PlaceOrder"), mapOf("orderId" to orderId))
    }
}
```
