```java
import io.cratis.chronicle.events.EventType;

@EventType
public record BookRegistered(String title, String isbn) {}

@EventType
public record BookBorrowed(String memberName) {}

@EventType
public record BookReturned() {}
```
