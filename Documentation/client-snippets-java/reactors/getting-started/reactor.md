```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;
import java.time.Instant;

interface ReactorEmailGateway {
    void sendOrderPlaced(String email, double amount, Instant occurred);
}

@Reactor
class OrderNotificationsReactor {
    private final ReactorEmailGateway emailGateway;

    OrderNotificationsReactor(ReactorEmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    void placed(ReactorOrderPlaced event, EventContext context) {
        emailGateway.sendOrderPlaced(
            event.customerEmail(),
            event.totalAmount(),
            context.getOccurred());
    }
}
```
