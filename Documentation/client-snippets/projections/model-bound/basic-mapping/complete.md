```kotlin title="Complete balance projection"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SubtractFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class BankAccountOpened(val accountName: String, val initialBalance: Double)

@EventType
data class BankAccountRenamed(val newName: String)

@EventType
data class FundsDeposited(val amount: Double)

@EventType
data class FundsWithdrawn(val amount: Double)

@ReadModel
@FromEvent(BankAccountOpened::class)
@FromEvent(BankAccountRenamed::class)
@FromEvent(FundsDeposited::class)
@FromEvent(FundsWithdrawn::class)
data class BankAccount(
    @SetFrom("accountName", BankAccountOpened::class)
    @SetFrom("newName", BankAccountRenamed::class)
    val name: String = "",

    @SetFrom("initialBalance", BankAccountOpened::class)
    @AddFrom(FundsDeposited::class, "amount")
    @SubtractFrom(FundsWithdrawn::class, "amount")
    val balance: Double = 0.0
)
```
