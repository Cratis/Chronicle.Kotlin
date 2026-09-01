```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@EventType
record TaggingReactorsOrderPlaced(String customerId, String orderId) {}

interface TaggingReactorsEmailService {
    void sendOrderConfirmation(String customerId, String orderId);
}

@Tag("Notifications")
@Reactor
class TaggingReactorsOrderConfirmationReactor {
    private final TaggingReactorsEmailService emailService;

    TaggingReactorsOrderConfirmationReactor(TaggingReactorsEmailService emailService) {
        this.emailService = emailService;
    }

    void placed(TaggingReactorsOrderPlaced event, EventContext context) {
        emailService.sendOrderConfirmation(event.customerId(), event.orderId());
    }
}
```
