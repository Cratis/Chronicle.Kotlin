```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "BookBorrowed")
record BookBorrowed(String memberName) {}

@EventType(id = "BookReturned")
record BookReturned() {}
```
