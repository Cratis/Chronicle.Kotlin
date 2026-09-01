```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "dotnet-client-customer-registered", generation = 2)
record MigrationsDotnetClientRenamedFromCustomerRegistered(String email) {
}

@EventType(id = "dotnet-client-customer-registered", generation = 1)
record MigrationsDotnetClientRenamedFromCustomerRegisteredV1(String emailAddress) {
}

class MigrationsDotnetClientRenamedFromCustomerRegisteredMigration
        extends EventTypeMigration<MigrationsDotnetClientRenamedFromCustomerRegistered,
                                   MigrationsDotnetClientRenamedFromCustomerRegisteredV1> {
    MigrationsDotnetClientRenamedFromCustomerRegisteredMigration() {
        super(MigrationsDotnetClientRenamedFromCustomerRegistered.class,
              MigrationsDotnetClientRenamedFromCustomerRegisteredV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientRenamedFromCustomerRegistered,
                                                 MigrationsDotnetClientRenamedFromCustomerRegisteredV1> builder) {
        builder.renamedFrom("email", "emailAddress");
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientRenamedFromCustomerRegisteredV1,
                                                   MigrationsDotnetClientRenamedFromCustomerRegistered> builder) {
        builder.renamedFrom("emailAddress", "email");
    }
}
```
