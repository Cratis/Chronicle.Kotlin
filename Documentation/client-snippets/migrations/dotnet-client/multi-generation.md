```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 3)
data class MigrationsDotnetClientMultiGenPersonRegistered(val email: String, val firstName: String, val lastName: String)

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 2)
data class MigrationsDotnetClientMultiGenPersonRegisteredV2(val email: String, val name: String)

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 1)
data class MigrationsDotnetClientMultiGenPersonRegisteredV1(val emailAddress: String, val name: String)

// Generation 1 -> 2: rename emailAddress to email
class MigrationsDotnetClientMultiGenPersonRegisteredV1ToV2 :
    EventTypeMigration<MigrationsDotnetClientMultiGenPersonRegisteredV2, MigrationsDotnetClientMultiGenPersonRegisteredV1>(
        MigrationsDotnetClientMultiGenPersonRegisteredV2::class,
        MigrationsDotnetClientMultiGenPersonRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV2, MigrationsDotnetClientMultiGenPersonRegisteredV1>) {
        builder.renamedFrom(MigrationsDotnetClientMultiGenPersonRegisteredV2::email, MigrationsDotnetClientMultiGenPersonRegisteredV1::emailAddress)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV1, MigrationsDotnetClientMultiGenPersonRegisteredV2>) {
        builder.renamedFrom(MigrationsDotnetClientMultiGenPersonRegisteredV1::emailAddress, MigrationsDotnetClientMultiGenPersonRegisteredV2::email)
    }
}

// Generation 2 -> 3: split name into firstName / lastName
class MigrationsDotnetClientMultiGenPersonRegisteredV2ToV3 :
    EventTypeMigration<MigrationsDotnetClientMultiGenPersonRegistered, MigrationsDotnetClientMultiGenPersonRegisteredV2>(
        MigrationsDotnetClientMultiGenPersonRegistered::class,
        MigrationsDotnetClientMultiGenPersonRegisteredV2::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegistered, MigrationsDotnetClientMultiGenPersonRegisteredV2>) {
        builder
            .split(MigrationsDotnetClientMultiGenPersonRegistered::firstName, MigrationsDotnetClientMultiGenPersonRegisteredV2::name, " ", 0)
            .split(MigrationsDotnetClientMultiGenPersonRegistered::lastName, MigrationsDotnetClientMultiGenPersonRegisteredV2::name, " ", 1)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV2, MigrationsDotnetClientMultiGenPersonRegistered>) {
        builder.combine(
            MigrationsDotnetClientMultiGenPersonRegisteredV2::name,
            " ",
            MigrationsDotnetClientMultiGenPersonRegistered::firstName,
            MigrationsDotnetClientMultiGenPersonRegistered::lastName
        )
    }
}
```
