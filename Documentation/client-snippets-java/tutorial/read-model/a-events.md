```java
import io.cratis.chronicle.events.EventType;

@EventType
record BookBorrowed(String memberName) {}

@EventType
record BookReturned() {}
```
