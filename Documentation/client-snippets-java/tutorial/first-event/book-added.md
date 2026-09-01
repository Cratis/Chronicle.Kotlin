```java
import io.cratis.chronicle.events.EventType;

@EventType
record BookAdded(String title, String isbn) {}
```
