```java
import io.cratis.chronicle.observation.Reactor;

@Reactor(id = "order-notifications")
class NamedOrderNotificationsReactor {
    void placed(ReactorOrderPlaced event) {
        // Perform the side effect.
    }
}
```
