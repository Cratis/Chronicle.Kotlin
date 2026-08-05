```kotlin
import io.cratis.chronicle.EventStore

suspend fun registerPayrollHttpService(store: EventStore) {
    store.externalServices.register("payroll-provider") { builder ->
        builder
            .http("https://payroll.example.com/api")
            .withBearerToken("payroll-integration-token")
    }
}
```
