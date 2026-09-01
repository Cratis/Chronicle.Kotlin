```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "side-effects-book-reserved-source")
record BookReserved(String memberId, String isbn) {}
```
