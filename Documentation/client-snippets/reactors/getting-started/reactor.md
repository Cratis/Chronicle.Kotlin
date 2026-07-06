```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import java.time.Instant

interface ReactorEmailGateway {
    fun sendOrderPlaced(email: String, amount: Double, occurred: Instant)
}

@Reactor
class OrderNotificationsReactor(private val emailGateway: ReactorEmailGateway) {
    fun placed(event: ReactorOrderPlaced, context: EventContext) {
        emailGateway.sendOrderPlaced(
            event.customerEmail,
            event.totalAmount,
            context.occurred)
    }
}
```
