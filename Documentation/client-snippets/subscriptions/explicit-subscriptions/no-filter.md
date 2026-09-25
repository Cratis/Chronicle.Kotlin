```kotlin
import io.cratis.chronicle.EventStore

suspend fun subscribeToEverything(store: EventStore) {
    // No withEventType calls — subscribe to event types currently registered by this client.
    store.eventStoreSubscriptions.subscribe("payroll-firehose", "PayrollEventStore") {
        // Intentionally left unconfigured.
    }
}
```
