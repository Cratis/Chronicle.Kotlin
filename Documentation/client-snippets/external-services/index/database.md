```kotlin
import io.cratis.chronicle.EventStore

suspend fun registerPayrollDatabase(store: EventStore) {
    store.externalServices.register("payroll-database") { builder ->
        builder.postgreSql(
            host = "payroll-db.internal",
            database = "payroll",
            username = "chronicle",
            password = "secret"
        )
    }
}
```
