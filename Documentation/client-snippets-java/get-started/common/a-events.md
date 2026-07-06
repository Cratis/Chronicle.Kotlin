```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "GetStartedBookAdded")
record GetStartedBookAdded(String title, String isbn) {}

@EventType(id = "GetStartedBookBorrowed")
record GetStartedBookBorrowed(String memberName) {}

@EventType(id = "GetStartedBookReturned")
record GetStartedBookReturned() {}
```
