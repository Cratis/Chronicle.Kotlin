```kotlin
import io.cratis.chronicle.EventStore

suspend fun subscribeWithStableId(store: EventStore) {
    // Use a stable, descriptive id — it identifies this subscription across restarts
    // and is how you target it later with unsubscribe().
    store.eventStoreSubscriptions.subscribe("payroll-inbox-v1", "PayrollEventStore") {
        // No filter configured here — see the filtering example for withEventType.
    }
}
```
