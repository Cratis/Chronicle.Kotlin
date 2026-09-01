```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@EventType
data class TaggingReactorsCustomerRegistered(val email: String, val name: String)

interface TaggingReactorsWelcomeEmailService {
    suspend fun sendWelcomeEmail(email: String, name: String)
}

@Tag("Notifications", "Customer", "Email")
@Reactor
class TaggingReactorsCustomerNotificationReactor(private val emailService: TaggingReactorsWelcomeEmailService) {
    suspend fun registered(event: TaggingReactorsCustomerRegistered, context: EventContext) =
        emailService.sendWelcomeEmail(event.email, event.name)
}
```
