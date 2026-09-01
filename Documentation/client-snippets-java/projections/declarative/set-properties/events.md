```java
import io.cratis.chronicle.events.EventType;

record DecSetPropsCustomer(String name, String email) {}

@EventType
record DecSetPropsAccountOpened(
    String number,
    DecSetPropsCustomer owner,
    String timestamp) {}

@EventType
record DecSetPropsMoneyDeposited(
    double amount,
    String timestamp) {}
```
