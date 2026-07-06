```java title="The event - an immutable fact"
import io.cratis.chronicle.events.EventType;

@EventType(id = "TestEvent")
record TestEvent(String message) {}
```
