```kotlin
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.events.EventType

@EventType(id = "subscriptions-explicit-filtering-payroll-run-completed")
data class SubscriptionsExplicitFilteringPayrollRunCompleted(val employeeId: String)

@EventType(id = "subscriptions-explicit-filtering-payroll-run-failed")
data class SubscriptionsExplicitFilteringPayrollRunFailed(val employeeId: String, val reason: String)

suspend fun subscribeToPayrollOutcomes(store: EventStore) {
    store.eventStoreSubscriptions.subscribe("payroll-outcomes", "PayrollEventStore") { builder ->
        builder
            .withEventType(SubscriptionsExplicitFilteringPayrollRunCompleted::class)
            .withEventType(SubscriptionsExplicitFilteringPayrollRunFailed::class)
    }
}
```
