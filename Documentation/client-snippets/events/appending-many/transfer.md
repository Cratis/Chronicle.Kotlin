```kotlin
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventToAppend
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType

@EventType
data class MoneyWithdrawn(val amount: Double)

@EventType
data class MoneyDeposited(val amount: Double)

class Transfers(private val eventLog: IEventLog) {
    /**
     * Moves money between two accounts as one atomic append - each event targets its own account,
     * and either both are committed or neither of them is.
     */
    suspend fun transfer(fromAccount: String, toAccount: String, amount: Double): List<AppendResult> =
        eventLog.appendMany(
            listOf(
                EventToAppend(fromAccount, MoneyWithdrawn(amount)),
                EventToAppend(toAccount, MoneyDeposited(amount))
            )
        )
}
```
