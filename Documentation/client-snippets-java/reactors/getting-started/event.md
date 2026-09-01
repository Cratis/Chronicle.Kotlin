```java
import io.cratis.chronicle.events.EventType;

@EventType
record ReactorOrderPlaced(String customerEmail, double totalAmount) {}
```
