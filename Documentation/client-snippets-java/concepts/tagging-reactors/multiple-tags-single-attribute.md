```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@EventType
record TaggingReactorsCustomerRegistered(String email, String name) {}

interface TaggingReactorsWelcomeEmailService {
    void sendWelcomeEmail(String email, String name);
}

@Tag({"Notifications", "Customer", "Email"})
@Reactor
class TaggingReactorsCustomerNotificationReactor {
    private final TaggingReactorsWelcomeEmailService emailService;

    TaggingReactorsCustomerNotificationReactor(TaggingReactorsWelcomeEmailService emailService) {
        this.emailService = emailService;
    }

    void registered(TaggingReactorsCustomerRegistered event, EventContext context) {
        emailService.sendWelcomeEmail(event.email(), event.name());
    }
}
```
