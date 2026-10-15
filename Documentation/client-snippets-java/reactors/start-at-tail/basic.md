```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.StartAtTail;

@EventType
record WelcomeEmailRequested(String email) {}

@Reactor
@StartAtTail
class SendWelcomeEmails {
    void requested(WelcomeEmailRequested event) {
        // Runs for events appended after this reactor was first registered.
        // It does not run for the history that was already in the event log.
    }
}
```
