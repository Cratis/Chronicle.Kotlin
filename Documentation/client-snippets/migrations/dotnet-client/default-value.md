```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "dotnet-client-task-created", generation = 2)
data class MigrationsDotnetClientDefaultValueTaskCreated(
    val title: String,
    val status: String,
    val retryCount: Int,
    val enabled: Boolean
)

@EventType(id = "dotnet-client-task-created", generation = 1)
data class MigrationsDotnetClientDefaultValueTaskCreatedV1(val title: String)

class MigrationsDotnetClientDefaultValueTaskCreatedMigration :
    EventTypeMigration<MigrationsDotnetClientDefaultValueTaskCreated, MigrationsDotnetClientDefaultValueTaskCreatedV1>(
        MigrationsDotnetClientDefaultValueTaskCreated::class,
        MigrationsDotnetClientDefaultValueTaskCreatedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientDefaultValueTaskCreated, MigrationsDotnetClientDefaultValueTaskCreatedV1>) {
        builder
            .defaultValue(MigrationsDotnetClientDefaultValueTaskCreated::status, "active")
            .defaultValue(MigrationsDotnetClientDefaultValueTaskCreated::retryCount, 0)
            .defaultValue(MigrationsDotnetClientDefaultValueTaskCreated::enabled, true)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientDefaultValueTaskCreatedV1, MigrationsDotnetClientDefaultValueTaskCreated>) {
        // status, retryCount, and enabled did not exist in generation 1 - nothing to map back
    }
}
```
