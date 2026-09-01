```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "crud-comparison-customer-registered")
record CrudComparisonCustomerRegistered(String name, String address) {}

@EventType(id = "crud-comparison-address-changed")
record CrudComparisonAddressChanged(String address) {}
```
