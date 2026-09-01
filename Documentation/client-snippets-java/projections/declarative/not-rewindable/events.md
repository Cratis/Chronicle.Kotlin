```java
import io.cratis.chronicle.events.EventType;

@EventType
record DecNotRewindableUserAction(
    String userId,
    String actionType,
    String details) {}

@EventType
record DecNotRewindableSystemEvent(
    String componentName,
    String eventType,
    String data) {}
```
