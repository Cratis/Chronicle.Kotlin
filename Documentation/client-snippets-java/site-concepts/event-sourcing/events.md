```java
import io.cratis.chronicle.events.EventType;

import java.math.BigDecimal;

@EventType
public record AccountOpened(String owner, BigDecimal initialBalance) {}

@EventType
public record DepositMade(BigDecimal amount) {}

@EventType
public record WithdrawalMade(BigDecimal amount) {}
```
