```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import java.time.Instant;

@EventType(id = "reactors-index-email-confirmed")
record ReactorsIndexEmailConfirmed(String email) {}

@Reactor
class ReactorsIndexEmailNotificationsReactor {
    void confirmed(ReactorsIndexEmailConfirmed event, EventContext context) {
        sendConfirmation(event.email(), context.getOccurred());
    }

    private void sendConfirmation(String email, Instant occurred) {}
}
```
