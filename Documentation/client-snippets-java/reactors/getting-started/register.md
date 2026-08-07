```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;
import kotlinx.coroutines.Job;

class ReactorRegistration {
    private final ReactorEmailGateway emailGateway;

    ReactorRegistration(ReactorEmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    Job register(IEventStore store) {
        return new BlockingEventStore(store)
            .getReactors()
            .register(new OrderNotificationsReactor(emailGateway));
    }
}
```
