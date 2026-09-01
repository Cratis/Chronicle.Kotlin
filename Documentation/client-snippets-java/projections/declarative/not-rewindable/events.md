```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "dec-not-rewindable-user-action")
record DecNotRewindableUserAction(
    String userId,
    String actionType,
    String details) {}

@EventType(id = "dec-not-rewindable-system-event")
record DecNotRewindableSystemEvent(
    String componentName,
    String eventType,
    String data) {}
```
