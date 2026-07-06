```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactors-index-email-confirmed")
data class ReactorsIndexEmailConfirmed(val email: String)

@Reactor
class ReactorsIndexEmailNotificationsReactor {
    fun confirmed(event: ReactorsIndexEmailConfirmed, context: EventContext) {
        sendConfirmation(event.email, context.occurred)
    }

    private fun sendConfirmation(email: String, occurred: java.time.Instant) {}
}
```
