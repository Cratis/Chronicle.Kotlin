```kotlin
import io.cratis.chronicle.IEventStore

class PassiveReducersReportingService(private val eventStore: IEventStore) {
    // This triggers the passive reducer to compute state from events
    suspend fun generateReport(reportId: String): PassiveReducersMonthlyRevenueReport? =
        eventStore.readModels.getInstanceByKey(PassiveReducersMonthlyRevenueReport::class, reportId)
}
```
