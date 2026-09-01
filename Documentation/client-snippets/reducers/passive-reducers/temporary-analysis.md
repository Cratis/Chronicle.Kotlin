```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType(id = "passive-reducers-transaction-completed")
data class PassiveReducersTransactionCompleted(val amount: Double)

@ReadModel
data class PassiveReducersAdHocReport(
    val totalRevenue: Double = 0.0,
    val transactionCount: Int = 0,
    val generatedAt: Instant = Instant.EPOCH
)

@Reducer(isActive = false)
class PassiveReducersAdHocReportReducer {
    fun completed(event: PassiveReducersTransactionCompleted, current: PassiveReducersAdHocReport?, context: EventContext): PassiveReducersAdHocReport {
        val revenue = current?.totalRevenue ?: 0.0
        val count = current?.transactionCount ?: 0

        return PassiveReducersAdHocReport(revenue + event.amount, count + 1, context.occurred)
    }
}
```
