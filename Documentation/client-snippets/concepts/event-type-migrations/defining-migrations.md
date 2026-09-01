```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "author-registered", generation = 2)
data class MigrationsAuthorRegistered(val firstName: String, val lastName: String)

@EventType(id = "author-registered", generation = 1)
data class MigrationsAuthorRegisteredV1(val name: String)

class MigrationsAuthorRegisteredMigration :
    EventTypeMigration<MigrationsAuthorRegistered, MigrationsAuthorRegisteredV1>(
        MigrationsAuthorRegistered::class,
        MigrationsAuthorRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsAuthorRegistered, MigrationsAuthorRegisteredV1>) {
        builder
            .split(MigrationsAuthorRegistered::firstName, MigrationsAuthorRegisteredV1::name, " ", 0)
            .split(MigrationsAuthorRegistered::lastName, MigrationsAuthorRegisteredV1::name, " ", 1)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsAuthorRegisteredV1, MigrationsAuthorRegistered>) {
        builder.combine(MigrationsAuthorRegisteredV1::name, " ", MigrationsAuthorRegistered::firstName, MigrationsAuthorRegistered::lastName)
    }
}
```
