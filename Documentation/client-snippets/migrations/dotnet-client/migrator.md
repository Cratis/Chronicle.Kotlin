```kotlin
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

class MigrationsDotnetClientAuthorRegisteredMigration :
    EventTypeMigration<MigrationsDotnetClientAuthorRegistered, MigrationsDotnetClientAuthorRegisteredV1>(
        MigrationsDotnetClientAuthorRegistered::class,
        MigrationsDotnetClientAuthorRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientAuthorRegistered, MigrationsDotnetClientAuthorRegisteredV1>) {
        builder
            .split(MigrationsDotnetClientAuthorRegistered::firstName, MigrationsDotnetClientAuthorRegisteredV1::name, " ", 0)
            .split(MigrationsDotnetClientAuthorRegistered::lastName, MigrationsDotnetClientAuthorRegisteredV1::name, " ", 1)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientAuthorRegisteredV1, MigrationsDotnetClientAuthorRegistered>) {
        builder.combine(
            MigrationsDotnetClientAuthorRegisteredV1::name,
            " ",
            MigrationsDotnetClientAuthorRegistered::firstName,
            MigrationsDotnetClientAuthorRegistered::lastName
        )
    }
}
```
