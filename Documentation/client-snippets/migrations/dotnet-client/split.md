```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "dotnet-client-person-registered", generation = 2)
data class MigrationsDotnetClientSplitPersonRegistered(val firstName: String, val lastName: String)

@EventType(id = "dotnet-client-person-registered", generation = 1)
data class MigrationsDotnetClientSplitPersonRegisteredV1(val fullName: String)

class MigrationsDotnetClientSplitPersonRegisteredMigration :
    EventTypeMigration<MigrationsDotnetClientSplitPersonRegistered, MigrationsDotnetClientSplitPersonRegisteredV1>(
        MigrationsDotnetClientSplitPersonRegistered::class,
        MigrationsDotnetClientSplitPersonRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientSplitPersonRegistered, MigrationsDotnetClientSplitPersonRegisteredV1>) {
        builder
            .split(MigrationsDotnetClientSplitPersonRegistered::firstName, MigrationsDotnetClientSplitPersonRegisteredV1::fullName, " ", 0)
            .split(MigrationsDotnetClientSplitPersonRegistered::lastName, MigrationsDotnetClientSplitPersonRegisteredV1::fullName, " ", 1)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientSplitPersonRegisteredV1, MigrationsDotnetClientSplitPersonRegistered>) {
        builder.combine(
            MigrationsDotnetClientSplitPersonRegisteredV1::fullName,
            " ",
            MigrationsDotnetClientSplitPersonRegistered::firstName,
            MigrationsDotnetClientSplitPersonRegistered::lastName
        )
    }
}
```
