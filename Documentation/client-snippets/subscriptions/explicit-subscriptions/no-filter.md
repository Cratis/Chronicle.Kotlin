```kotlin
import io.cratis.chronicle.EventStore

suspend fun subscribeToEverything(store: EventStore) {
    // No withEventType calls — every event type from the source outbox is subscribed to.
    store.eventStoreSubscriptions.subscribe("payroll-firehose", "PayrollEventStore") {
        // Intentionally left unconfigured.
    }
}
```
