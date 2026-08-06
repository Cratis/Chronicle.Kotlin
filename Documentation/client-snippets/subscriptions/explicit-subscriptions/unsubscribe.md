```kotlin
import io.cratis.chronicle.EventStore

suspend fun unsubscribeFromPayroll(store: EventStore) {
    store.eventStoreSubscriptions.unsubscribe("payroll-inbox")
}
```
