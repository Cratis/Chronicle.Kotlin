```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "dotnet-client-task-created", generation = 2)
record MigrationsDotnetClientDefaultValueTaskCreated(
        String title,
        String status,
        int retryCount,
        boolean enabled) {
}

@EventType(id = "dotnet-client-task-created", generation = 1)
record MigrationsDotnetClientDefaultValueTaskCreatedV1(String title) {
}

class MigrationsDotnetClientDefaultValueTaskCreatedMigration
        extends EventTypeMigration<MigrationsDotnetClientDefaultValueTaskCreated,
                                   MigrationsDotnetClientDefaultValueTaskCreatedV1> {
    MigrationsDotnetClientDefaultValueTaskCreatedMigration() {
        super(MigrationsDotnetClientDefaultValueTaskCreated.class,
              MigrationsDotnetClientDefaultValueTaskCreatedV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientDefaultValueTaskCreated,
                                                 MigrationsDotnetClientDefaultValueTaskCreatedV1> builder) {
        builder
            .defaultValue("status", "active")
            .defaultValue("retryCount", 0)
            .defaultValue("enabled", true);
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientDefaultValueTaskCreatedV1,
                                                   MigrationsDotnetClientDefaultValueTaskCreated> builder) {
        // status, retryCount, and enabled did not exist in generation 1 - nothing to map back
    }
}
```
