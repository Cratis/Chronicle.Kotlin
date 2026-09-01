```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "dec-event-context-user-logged-in")
record DecEventContextUserLoggedIn(String username) {}

@EventType(id = "dec-event-context-user-performed-action")
record DecEventContextUserPerformedAction(String userId, String actionType) {}
```
