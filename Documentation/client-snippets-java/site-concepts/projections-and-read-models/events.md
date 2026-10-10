```java
import io.cratis.chronicle.events.EventType;

@EventType
record BookRegistered(String title, String isbn) {}

@EventType
record BookBorrowed(String memberName) {}

@EventType
record BookReturned() {}
```
