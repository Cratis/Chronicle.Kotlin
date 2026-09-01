```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@EventType
data class TaggingReactorsOrderShipped(val phoneNumber: String, val trackingNumber: String)

interface TaggingReactorsSmsService {
    suspend fun sendShippingNotification(phoneNumber: String, trackingNumber: String)
}

@Tag("Notifications", "SMS")
@Tag("Customer")
@Reactor
class TaggingReactorsSmsNotificationReactor(private val smsService: TaggingReactorsSmsService) {
    suspend fun shipped(event: TaggingReactorsOrderShipped, context: EventContext) =
        smsService.sendShippingNotification(event.phoneNumber, event.trackingNumber)
}
```
