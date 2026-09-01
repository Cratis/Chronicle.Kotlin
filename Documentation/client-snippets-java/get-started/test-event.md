```java title="The event - an immutable fact"
import io.cratis.chronicle.events.EventType;

@EventType
record TestEvent(String message) {}
```
