```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 3)
record MigrationsDotnetClientMultiGenPersonRegistered(String email, String firstName, String lastName) {
}

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 2)
record MigrationsDotnetClientMultiGenPersonRegisteredV2(String email, String name) {
}

@EventType(id = "dotnet-client-multi-gen-person-registered", generation = 1)
record MigrationsDotnetClientMultiGenPersonRegisteredV1(String emailAddress, String name) {
}

// Generation 1 -> 2: rename emailAddress to email
class MigrationsDotnetClientMultiGenPersonRegisteredV1ToV2
        extends EventTypeMigration<MigrationsDotnetClientMultiGenPersonRegisteredV2,
                                   MigrationsDotnetClientMultiGenPersonRegisteredV1> {
    MigrationsDotnetClientMultiGenPersonRegisteredV1ToV2() {
        super(MigrationsDotnetClientMultiGenPersonRegisteredV2.class,
              MigrationsDotnetClientMultiGenPersonRegisteredV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV2,
                                                 MigrationsDotnetClientMultiGenPersonRegisteredV1> builder) {
        builder.renamedFrom("email", "emailAddress");
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV1,
                                                   MigrationsDotnetClientMultiGenPersonRegisteredV2> builder) {
        builder.renamedFrom("emailAddress", "email");
    }
}

// Generation 2 -> 3: split name into firstName / lastName
class MigrationsDotnetClientMultiGenPersonRegisteredV2ToV3
        extends EventTypeMigration<MigrationsDotnetClientMultiGenPersonRegistered,
                                   MigrationsDotnetClientMultiGenPersonRegisteredV2> {
    MigrationsDotnetClientMultiGenPersonRegisteredV2ToV3() {
        super(MigrationsDotnetClientMultiGenPersonRegistered.class,
              MigrationsDotnetClientMultiGenPersonRegisteredV2.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegistered,
                                                 MigrationsDotnetClientMultiGenPersonRegisteredV2> builder) {
        builder
            .split("firstName", "name", " ", 0)
            .split("lastName", "name", " ", 1);
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientMultiGenPersonRegisteredV2,
                                                   MigrationsDotnetClientMultiGenPersonRegistered> builder) {
        builder.combine("name", " ", "firstName", "lastName");
    }
}
```
