```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType
data class ReducersIndexDepositMade(val amount: Double)

@EventType
data class ReducersIndexWithdrawalMade(val amount: Double)

@ReadModel
data class ReducersIndexAccountBalance(val balance: Double = 0.0, val lastUpdated: Instant = Instant.EPOCH)

@Reducer
class ReducersIndexAccountBalanceReducer {
    fun deposited(event: ReducersIndexDepositMade, current: ReducersIndexAccountBalance?): ReducersIndexAccountBalance {
        val currentBalance = current?.balance ?: 0.0
        return ReducersIndexAccountBalance(currentBalance + event.amount, Instant.now())
    }

    fun withdrawalMade(event: ReducersIndexWithdrawalMade, current: ReducersIndexAccountBalance?): ReducersIndexAccountBalance {
        val currentBalance = current?.balance ?: 0.0
        return ReducersIndexAccountBalance(currentBalance - event.amount, Instant.now())
    }
}
```
