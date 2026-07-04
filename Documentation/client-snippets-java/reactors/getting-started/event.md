```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "ReactorOrderPlaced")
record ReactorOrderPlaced(String customerEmail, double totalAmount) {}
```
