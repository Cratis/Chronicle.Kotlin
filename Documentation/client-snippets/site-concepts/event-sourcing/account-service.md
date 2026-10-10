```kotlin
import io.cratis.chronicle.eventSequences.IEventLog

class AccountService(private val eventLog: IEventLog) {
    suspend fun withdraw(accountId: String, amount: Double) {
        val result = eventLog.append(accountId, WithdrawalMade(amount))

        if (!result.isSuccess) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
