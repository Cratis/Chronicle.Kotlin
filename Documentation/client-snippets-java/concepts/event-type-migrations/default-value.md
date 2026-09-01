```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "order-shipped", generation = 2)
record MigrationsDefaultValueOrderShipped(String trackingNumber, int retryCount, String description) {
}

@EventType(id = "order-shipped", generation = 1)
record MigrationsDefaultValueOrderShippedV1(String trackingNumber) {
}

class MigrationsDefaultValueOrderShippedMigration
        extends EventTypeMigration<MigrationsDefaultValueOrderShipped, MigrationsDefaultValueOrderShippedV1> {
    MigrationsDefaultValueOrderShippedMigration() {
        super(MigrationsDefaultValueOrderShipped.class, MigrationsDefaultValueOrderShippedV1.class);
    }

    @Override
    public void upcast(
            EventTypeMigrationBuilder<MigrationsDefaultValueOrderShipped, MigrationsDefaultValueOrderShippedV1> builder) {
        builder
            .defaultValue("retryCount", 42)
            .defaultValue("description", "default string");
    }

    @Override
    public void downcast(
            EventTypeMigrationBuilder<MigrationsDefaultValueOrderShippedV1, MigrationsDefaultValueOrderShipped> builder) {
        // retryCount and description did not exist in generation 1 — nothing to map back
    }
}
```
