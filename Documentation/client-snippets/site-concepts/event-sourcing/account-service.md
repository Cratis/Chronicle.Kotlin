```kotlin
import io.cratis.chronicle.eventSequences.IEventLog
import java.math.BigDecimal

class AccountService(private val eventLog: IEventLog) {
    suspend fun withdraw(accountId: String, amount: BigDecimal) {
        val result = eventLog.append(accountId, WithdrawalMade(amount))

        if (!result.isSuccess) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
