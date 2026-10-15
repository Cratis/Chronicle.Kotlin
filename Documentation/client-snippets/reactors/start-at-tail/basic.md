```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.StartAtTail

@EventType
data class WelcomeEmailRequested(val email: String)

@Reactor
@StartAtTail
class SendWelcomeEmails {
    fun requested(event: WelcomeEmailRequested) {
        // Runs for events appended after this reactor was first registered.
        // It does not run for the history that was already in the event log.
    }
}
```
