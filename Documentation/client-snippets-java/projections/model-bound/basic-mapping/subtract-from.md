```java title="Subtract from an event"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.AddFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SubtractFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "balance-account-opened")
record BalanceAccountOpened(double initialBalance) {}

@EventType(id = "balance-deposit-made")
record BalanceDepositMade(double amount) {}

@EventType(id = "balance-withdrawal-made")
record BalanceWithdrawalMade(double amount) {}

@ReadModel
@FromEvent(eventType = BalanceAccountOpened.class)
@FromEvent(eventType = BalanceDepositMade.class)
@FromEvent(eventType = BalanceWithdrawalMade.class)
class BalanceAccount {
    @SetFrom(propertyPath = "initialBalance", eventType = BalanceAccountOpened.class)
    @AddFrom(eventType = BalanceDepositMade.class, eventPropertyName = "amount")
    @SubtractFrom(eventType = BalanceWithdrawalMade.class, eventPropertyName = "amount")
    public double balance = 0.0;
}
```
