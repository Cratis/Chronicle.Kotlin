```java
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

class MigrationsDotnetClientAuthorRegisteredMigration
        extends EventTypeMigration<MigrationsDotnetClientAuthorRegistered,
                                   MigrationsDotnetClientAuthorRegisteredV1> {
    MigrationsDotnetClientAuthorRegisteredMigration() {
        super(MigrationsDotnetClientAuthorRegistered.class, MigrationsDotnetClientAuthorRegisteredV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientAuthorRegistered,
                                                 MigrationsDotnetClientAuthorRegisteredV1> builder) {
        builder
            .split("firstName", "name", " ", 0)
            .split("lastName", "name", " ", 1);
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientAuthorRegisteredV1,
                                                   MigrationsDotnetClientAuthorRegistered> builder) {
        builder.combine("name", " ", "firstName", "lastName");
    }
}
```
