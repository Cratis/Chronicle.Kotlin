```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@EventType
data class TaggingReactorsOrderPlaced(val customerId: String, val orderId: String)

interface TaggingReactorsEmailService {
    suspend fun sendOrderConfirmation(customerId: String, orderId: String)
}

@Tag("Notifications")
@Reactor
class TaggingReactorsOrderConfirmationReactor(private val emailService: TaggingReactorsEmailService) {
    suspend fun placed(event: TaggingReactorsOrderPlaced, context: EventContext) =
        emailService.sendOrderConfirmation(event.customerId, event.orderId)
}
```
