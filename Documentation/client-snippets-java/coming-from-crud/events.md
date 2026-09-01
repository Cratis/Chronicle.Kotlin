```java
import io.cratis.chronicle.events.EventType;

@EventType
record CrudComparisonCustomerRegistered(String name, String address) {}

@EventType
record CrudComparisonAddressChanged(String address) {}
```
