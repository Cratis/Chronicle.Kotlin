```java
import io.cratis.chronicle.events.EventType;

@EventType
record PdlAutoMapUserRegistered(String name, String email, int age) {}
```
