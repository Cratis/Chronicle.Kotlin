```java
import io.cratis.chronicle.events.EventType;

@EventType
record BookReserved(String memberId, String isbn) {}
```
