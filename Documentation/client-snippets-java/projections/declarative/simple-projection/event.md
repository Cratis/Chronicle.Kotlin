```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "dec-simple-user-created")
record DecSimpleUserCreated(String name, String email, String createdAt) {}
```
