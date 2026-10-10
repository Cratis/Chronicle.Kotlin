```java
import io.cratis.chronicle.events.EventType;

@EventType
record AccountOpened(String owner, double initialBalance) {}

@EventType
record DepositMade(double amount) {}

@EventType
record WithdrawalMade(double amount) {}
```
