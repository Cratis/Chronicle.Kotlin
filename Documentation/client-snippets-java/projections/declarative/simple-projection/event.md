```java
import io.cratis.chronicle.events.EventType;

@EventType
record DecSimpleUserCreated(String name, String email, String createdAt) {}
```
