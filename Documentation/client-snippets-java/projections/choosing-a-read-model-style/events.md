```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventType;

@EventType
record ChoosingStyleBookRegistered(String title, String isbn) {}

@EventType
record ChoosingStyleBookBorrowed(@Pii String memberName) {}

@EventType
record ChoosingStyleBookReturned() {}
```
