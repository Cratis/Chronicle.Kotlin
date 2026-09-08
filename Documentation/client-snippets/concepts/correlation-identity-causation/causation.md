```kotlin
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import java.time.Instant

class CorrelationIdentityCausationCausation {
    fun recordPlaceOrder(context: OperationContext, orderId: String): OperationContext =
        context.causedBy(
            Causation(Instant.now(), CausationType("MyApp.Commands.PlaceOrder"), mapOf("orderId" to orderId))
        )
}
```
