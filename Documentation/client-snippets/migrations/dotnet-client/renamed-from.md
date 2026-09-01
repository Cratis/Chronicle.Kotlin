```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "dotnet-client-customer-registered", generation = 2)
data class MigrationsDotnetClientRenamedFromCustomerRegistered(val email: String)

@EventType(id = "dotnet-client-customer-registered", generation = 1)
data class MigrationsDotnetClientRenamedFromCustomerRegisteredV1(val emailAddress: String)

class MigrationsDotnetClientRenamedFromCustomerRegisteredMigration :
    EventTypeMigration<MigrationsDotnetClientRenamedFromCustomerRegistered, MigrationsDotnetClientRenamedFromCustomerRegisteredV1>(
        MigrationsDotnetClientRenamedFromCustomerRegistered::class,
        MigrationsDotnetClientRenamedFromCustomerRegisteredV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientRenamedFromCustomerRegistered, MigrationsDotnetClientRenamedFromCustomerRegisteredV1>) {
        builder.renamedFrom(MigrationsDotnetClientRenamedFromCustomerRegistered::email, MigrationsDotnetClientRenamedFromCustomerRegisteredV1::emailAddress)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientRenamedFromCustomerRegisteredV1, MigrationsDotnetClientRenamedFromCustomerRegistered>) {
        builder.renamedFrom(MigrationsDotnetClientRenamedFromCustomerRegisteredV1::emailAddress, MigrationsDotnetClientRenamedFromCustomerRegistered::email)
    }
}
```
