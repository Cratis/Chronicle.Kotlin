```kotlin title="Subtract from an event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SubtractFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "balance-account-opened")
data class BalanceAccountOpened(val initialBalance: Double)

@EventType(id = "balance-deposit-made")
data class BalanceDepositMade(val amount: Double)

@EventType(id = "balance-withdrawal-made")
data class BalanceWithdrawalMade(val amount: Double)

@ReadModel
@FromEvent(BalanceAccountOpened::class)
@FromEvent(BalanceDepositMade::class)
@FromEvent(BalanceWithdrawalMade::class)
data class BalanceAccount(
    @SetFrom("initialBalance", BalanceAccountOpened::class)
    @AddFrom(BalanceDepositMade::class, "amount")
    @SubtractFrom(BalanceWithdrawalMade::class, "amount")
    val balance: Double = 0.0
)
```
