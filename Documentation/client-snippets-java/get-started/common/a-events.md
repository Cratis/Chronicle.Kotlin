```java
import io.cratis.chronicle.events.EventType;

@EventType
record GetStartedBookAdded(String title, String isbn) {}

@EventType
record GetStartedBookBorrowed(String memberName) {}

@EventType
record GetStartedBookReturned() {}
```
