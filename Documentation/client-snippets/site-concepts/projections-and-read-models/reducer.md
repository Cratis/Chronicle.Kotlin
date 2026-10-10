```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType
data class DepositMade(val amount: Double)

@EventType
data class WithdrawalMade(val amount: Double)

@ReadModel
data class AccountBalance(val balance: Double = 0.0, val lastUpdated: Instant = Instant.EPOCH)

@Reducer
class AccountBalanceReducer {
    fun deposited(event: DepositMade, current: AccountBalance?, context: EventContext) =
        AccountBalance((current?.balance ?: 0.0) + event.amount, context.occurred)

    fun withdrawalMade(event: WithdrawalMade, current: AccountBalance?, context: EventContext) =
        AccountBalance((current?.balance ?: 0.0) - event.amount, context.occurred)
}
```
