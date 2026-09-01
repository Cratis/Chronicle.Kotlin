```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "migrations-validation-order-placed", generation = 1)
record MigrationsValidationOrderPlacedV1(String orderId) {
}

@EventType(id = "migrations-validation-order-placed", generation = 2)
record MigrationsValidationOrderPlacedV2(String orderId, String currency) {
}

class MigrationsValidationOrderPlacedMigration
        extends EventTypeMigration<MigrationsValidationOrderPlacedV2, MigrationsValidationOrderPlacedV1> {
    MigrationsValidationOrderPlacedMigration() {
        super(MigrationsValidationOrderPlacedV2.class, MigrationsValidationOrderPlacedV1.class);
    }

    @Override
    public void upcast(
            EventTypeMigrationBuilder<MigrationsValidationOrderPlacedV2, MigrationsValidationOrderPlacedV1> builder) {
        builder.defaultValue("currency", "USD");
    }
}
```
