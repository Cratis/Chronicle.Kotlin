```java title="Add from an event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.AddFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record AccountOpenedForDeposits(double initialBalance) {}

@EventType
record DepositMadeForBalance(double amount) {}

@ReadModel
@FromEvent(eventType = AccountOpenedForDeposits.class)
@FromEvent(eventType = DepositMadeForBalance.class)
class DepositAccount {
    @SetFrom(propertyPath = "initialBalance", eventType = AccountOpenedForDeposits.class)
    @AddFrom(eventType = DepositMadeForBalance.class, eventPropertyName = "amount")
    public double balance = 0.0;
}
```
