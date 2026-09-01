```java
import io.cratis.chronicle.events.EventType;

record DecSetPropsCustomer(String name, String email) {}

@EventType(id = "dec-set-props-account-opened")
record DecSetPropsAccountOpened(
    String number,
    DecSetPropsCustomer owner,
    String timestamp) {}

@EventType(id = "dec-set-props-money-deposited")
record DecSetPropsMoneyDeposited(
    double amount,
    String timestamp) {}
```
