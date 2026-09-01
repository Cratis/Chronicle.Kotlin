```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "passive-reducers-payment-received")
data class PassiveReducersPaymentReceived(val category: String, val amount: Double)

@ReadModel
data class PassiveReducersMonthlyRevenueReport(
    val totalRevenue: Double = 0.0,
    val revenueByCategory: Map<String, Double> = emptyMap(),
    val month: Int = 0,
    val year: Int = 0
)

@Reducer(isActive = false)
class PassiveReducersMonthlyRevenueReportReducer {
    fun received(event: PassiveReducersPaymentReceived, current: PassiveReducersMonthlyRevenueReport?, context: EventContext): PassiveReducersMonthlyRevenueReport {
        val revenue = current?.totalRevenue ?: 0.0
        val byCategory = current?.revenueByCategory ?: emptyMap()
        val updatedCategory = (byCategory[event.category] ?: 0.0) + event.amount
        val zoned = context.occurred.atZone(java.time.ZoneOffset.UTC)

        return PassiveReducersMonthlyRevenueReport(
            revenue + event.amount,
            byCategory + (event.category to updatedCategory),
            zoned.monthValue,
            zoned.year
        )
    }
}
```
