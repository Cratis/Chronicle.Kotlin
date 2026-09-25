```kotlin
import io.cratis.chronicle.EventStore

suspend fun registerPayrollDatabase(store: EventStore) {
    store.externalServices.register("payroll-database") { builder ->
        builder.postgreSql(
            host = "payroll-db.internal",
            database = "payroll",
            username = "chronicle",
            password = System.getenv("CHRONICLE_PAYROLL_DB_PASSWORD")
                ?: error("CHRONICLE_PAYROLL_DB_PASSWORD is required")
        )
    }
}
```
