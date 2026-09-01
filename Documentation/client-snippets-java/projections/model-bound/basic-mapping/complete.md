```java title="Complete balance projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.AddFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SubtractFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record BankAccountOpened(String accountName, double initialBalance) {}

@EventType
record BankAccountRenamed(String newName) {}

@EventType
record FundsDeposited(double amount) {}

@EventType
record FundsWithdrawn(double amount) {}

@ReadModel
@FromEvent(eventType = BankAccountOpened.class)
@FromEvent(eventType = BankAccountRenamed.class)
@FromEvent(eventType = FundsDeposited.class)
@FromEvent(eventType = FundsWithdrawn.class)
class BankAccount {
    @SetFrom(propertyPath = "accountName", eventType = BankAccountOpened.class)
    @SetFrom(propertyPath = "newName", eventType = BankAccountRenamed.class)
    public String name = "";

    @SetFrom(propertyPath = "initialBalance", eventType = BankAccountOpened.class)
    @AddFrom(eventType = FundsDeposited.class, eventPropertyName = "amount")
    @SubtractFrom(eventType = FundsWithdrawn.class, eventPropertyName = "amount")
    public double balance = 0.0;
}
```
