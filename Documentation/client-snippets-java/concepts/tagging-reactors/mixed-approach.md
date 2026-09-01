```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@EventType
record TaggingReactorsOrderShipped(String phoneNumber, String trackingNumber) {}

interface TaggingReactorsSmsService {
    void sendShippingNotification(String phoneNumber, String trackingNumber);
}

@Tag({"Notifications", "SMS"})
@Tag("Customer")
@Reactor
class TaggingReactorsSmsNotificationReactor {
    private final TaggingReactorsSmsService smsService;

    TaggingReactorsSmsNotificationReactor(TaggingReactorsSmsService smsService) {
        this.smsService = smsService;
    }

    void shipped(TaggingReactorsOrderShipped event, EventContext context) {
        smsService.sendShippingNotification(event.phoneNumber(), event.trackingNumber());
    }
}
```
