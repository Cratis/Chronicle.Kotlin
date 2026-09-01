```java
import io.cratis.chronicle.events.EventType;

@EventType
record DecEventContextUserLoggedIn(String username) {}

@EventType
record DecEventContextUserPerformedAction(String userId, String actionType) {}
```
