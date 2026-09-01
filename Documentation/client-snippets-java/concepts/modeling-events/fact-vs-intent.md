```java
import io.cratis.chronicle.events.EventType;

record ModelingEventsAddress(String street, String city) {}

// A fact that happened
@EventType
record ModelingEventsAddressChanged(ModelingEventsAddress address) {}

// An intent (that's a command) or a state blob (that's a read model) — not an event
@EventType
record ModelingEventsUpdateAddress(ModelingEventsAddress address) {}
```
