```java
import io.cratis.chronicle.events.EventType;

@EventType
record ChoosingStyleBookRegistered(String title, String isbn) {}

@EventType
record ChoosingStyleBookBorrowed(String memberName) {}

@EventType
record ChoosingStyleBookReturned() {}
```
