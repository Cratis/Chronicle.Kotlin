```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "BookAdded")
record BookAdded(String title, String isbn) {}
```
