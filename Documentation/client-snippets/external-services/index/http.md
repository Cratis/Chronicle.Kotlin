```kotlin
import io.cratis.chronicle.EventStore

suspend fun registerPayrollHttpService(store: EventStore) {
    store.externalServices.register("payroll-provider") { builder ->
        builder
            .http("https://payroll.example.com/api")
            .withBearerToken(System.getenv("CHRONICLE_PAYROLL_TOKEN") ?: error("CHRONICLE_PAYROLL_TOKEN is required"))
    }
}
```
