```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.math.BigDecimal
import java.time.Instant

@EventType
data class DepositMade(val amount: BigDecimal)

@EventType
data class WithdrawalMade(val amount: BigDecimal)

@ReadModel
data class AccountBalance(val balance: BigDecimal = BigDecimal.ZERO, val lastUpdated: Instant = Instant.EPOCH)

@Reducer
class AccountBalanceReducer {
    fun deposited(event: DepositMade, current: AccountBalance?, context: EventContext) =
        AccountBalance((current?.balance ?: BigDecimal.ZERO) + event.amount, context.occurred)

    fun withdrawalMade(event: WithdrawalMade, current: AccountBalance?, context: EventContext) =
        AccountBalance((current?.balance ?: BigDecimal.ZERO) - event.amount, context.occurred)
}
```
