```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "dotnet-client-person-registered", generation = 2)
record MigrationsDotnetClientSplitPersonRegistered(String firstName, String lastName) {
}

@EventType(id = "dotnet-client-person-registered", generation = 1)
record MigrationsDotnetClientSplitPersonRegisteredV1(String fullName) {
}

class MigrationsDotnetClientSplitPersonRegisteredMigration
        extends EventTypeMigration<MigrationsDotnetClientSplitPersonRegistered,
                                   MigrationsDotnetClientSplitPersonRegisteredV1> {
    MigrationsDotnetClientSplitPersonRegisteredMigration() {
        super(MigrationsDotnetClientSplitPersonRegistered.class,
              MigrationsDotnetClientSplitPersonRegisteredV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientSplitPersonRegistered,
                                                 MigrationsDotnetClientSplitPersonRegisteredV1> builder) {
        builder
            .split("firstName", "fullName", " ", 0)
            .split("lastName", "fullName", " ", 1);
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientSplitPersonRegisteredV1,
                                                   MigrationsDotnetClientSplitPersonRegistered> builder) {
        builder.combine("fullName", " ", "firstName", "lastName");
    }
}
```
