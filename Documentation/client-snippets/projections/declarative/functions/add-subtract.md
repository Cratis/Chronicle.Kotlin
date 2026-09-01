```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.math.BigDecimal

@EventType(id = "dec-functions-account-opened")
data class DecFunctionsAccountOpened(val number: String)

@EventType(id = "dec-functions-money-deposited")
data class DecFunctionsMoneyDeposited(val amount: BigDecimal)

@EventType(id = "dec-functions-money-withdrawn")
data class DecFunctionsMoneyWithdrawn(val amount: BigDecimal)

data class DecFunctionsAccount(val number: String = "", val balance: BigDecimal = BigDecimal.ZERO)

class DecFunctionsAccountProjection : IProjectionFor<DecFunctionsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecFunctionsAccount>) {
        builder
            .autoMap()
            .from(DecFunctionsAccountOpened::class)
            .from(DecFunctionsMoneyDeposited::class) {
                it.add(DecFunctionsAccount::balance).with("amount")
            }
            .from(DecFunctionsMoneyWithdrawn::class) {
                it.subtract(DecFunctionsAccount::balance).with("amount")
            }
    }
}
```
