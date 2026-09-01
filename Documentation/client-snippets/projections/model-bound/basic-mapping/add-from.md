```kotlin title="Add from an event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class AccountOpenedForDeposits(val initialBalance: Double)

@EventType
data class DepositMadeForBalance(val amount: Double)

@ReadModel
@FromEvent(AccountOpenedForDeposits::class)
@FromEvent(DepositMadeForBalance::class)
data class DepositAccount(
    @SetFrom("initialBalance", AccountOpenedForDeposits::class)
    @AddFrom(DepositMadeForBalance::class, "amount")
    val balance: Double = 0.0
)
```
