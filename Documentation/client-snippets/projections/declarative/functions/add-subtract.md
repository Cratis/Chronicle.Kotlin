```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.math.BigDecimal

@EventType
data class DecFunctionsAccountOpened(val number: String)

@EventType
data class DecFunctionsMoneyDeposited(val amount: BigDecimal)

@EventType
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
