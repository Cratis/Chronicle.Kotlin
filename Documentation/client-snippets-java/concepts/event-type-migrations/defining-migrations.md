```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "author-registered", generation = 2)
record MigrationsAuthorRegistered(String firstName, String lastName) {
}

@EventType(id = "author-registered", generation = 1)
record MigrationsAuthorRegisteredV1(String name) {
}

class MigrationsAuthorRegisteredMigration
        extends EventTypeMigration<MigrationsAuthorRegistered, MigrationsAuthorRegisteredV1> {
    MigrationsAuthorRegisteredMigration() {
        super(MigrationsAuthorRegistered.class, MigrationsAuthorRegisteredV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsAuthorRegistered, MigrationsAuthorRegisteredV1> builder) {
        builder
            .split("firstName", "name", " ", 0)
            .split("lastName", "name", " ", 1);
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsAuthorRegisteredV1, MigrationsAuthorRegistered> builder) {
        builder.combine("name", " ", "firstName", "lastName");
    }
}
```
