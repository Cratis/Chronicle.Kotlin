```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "person-registered", generation = 2)
record MigrationsSplitPersonRegistered(String firstName, String lastName) {
}

@EventType(id = "person-registered", generation = 1)
record MigrationsSplitPersonRegisteredV1(String fullName) {
}

class MigrationsSplitPersonRegisteredMigration
        extends EventTypeMigration<MigrationsSplitPersonRegistered, MigrationsSplitPersonRegisteredV1> {
    MigrationsSplitPersonRegisteredMigration() {
        super(MigrationsSplitPersonRegistered.class, MigrationsSplitPersonRegisteredV1.class);
    }

    @Override
    public void upcast(
            EventTypeMigrationBuilder<MigrationsSplitPersonRegistered, MigrationsSplitPersonRegisteredV1> builder) {
        builder
            .split("firstName", "fullName", " ", 0)  // Gets first part
            .split("lastName", "fullName", " ", 1);  // Gets second part
    }

    @Override
    public void downcast(
            EventTypeMigrationBuilder<MigrationsSplitPersonRegisteredV1, MigrationsSplitPersonRegistered> builder) {
        builder.combine("fullName", " ", "firstName", "lastName");
    }
}
```
