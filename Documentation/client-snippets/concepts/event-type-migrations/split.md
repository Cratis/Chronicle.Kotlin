```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "person-registered", generation = 2)
data class MigrationsSplitPersonRegistered(val firstName: String, val lastName: String)

@EventType(id = "person-registered", generation = 1)
data class MigrationsSplitPersonRegisteredV1(val fullName: String)

class MigrationsSplitPersonRegisteredMigration :
    EventTypeMigration<MigrationsSplitPersonRegistered, MigrationsSplitPersonRegisteredV1>(
        MigrationsSplitPersonRegistered::class,
        MigrationsSplitPersonRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsSplitPersonRegistered, MigrationsSplitPersonRegisteredV1>) {
        builder
            .split(MigrationsSplitPersonRegistered::firstName, MigrationsSplitPersonRegisteredV1::fullName, " ", 0) // Gets first part
            .split(MigrationsSplitPersonRegistered::lastName, MigrationsSplitPersonRegisteredV1::fullName, " ", 1) // Gets second part
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsSplitPersonRegisteredV1, MigrationsSplitPersonRegistered>) {
        builder.combine(MigrationsSplitPersonRegisteredV1::fullName, " ", MigrationsSplitPersonRegistered::firstName, MigrationsSplitPersonRegistered::lastName)
    }
}
```
