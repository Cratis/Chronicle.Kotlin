```kotlin
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.events.EventType

@EventType(id = "subscriptions-explicit-payroll-run-completed")
data class SubscriptionsExplicitPayrollRunCompleted(val employeeId: String, val amount: Double)

suspend fun subscribeToPayroll(store: EventStore) {
    store.eventStoreSubscriptions.subscribe("payroll-inbox", "PayrollEventStore") { builder ->
        builder.withEventType(SubscriptionsExplicitPayrollRunCompleted::class)
    }
}
```
