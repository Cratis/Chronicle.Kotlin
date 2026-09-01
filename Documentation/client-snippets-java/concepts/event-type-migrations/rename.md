```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "payment-processed", generation = 2)
record MigrationsRenamePaymentProcessed(double amount) {
}

@EventType(id = "payment-processed", generation = 1)
record MigrationsRenamePaymentProcessedV1(double oldAmount) {
}

class MigrationsRenamePaymentProcessedMigration
        extends EventTypeMigration<MigrationsRenamePaymentProcessed, MigrationsRenamePaymentProcessedV1> {
    MigrationsRenamePaymentProcessedMigration() {
        super(MigrationsRenamePaymentProcessed.class, MigrationsRenamePaymentProcessedV1.class);
    }

    @Override
    public void upcast(
            EventTypeMigrationBuilder<MigrationsRenamePaymentProcessed, MigrationsRenamePaymentProcessedV1> builder) {
        builder.renamedFrom("amount", "oldAmount");
    }

    @Override
    public void downcast(
            EventTypeMigrationBuilder<MigrationsRenamePaymentProcessedV1, MigrationsRenamePaymentProcessed> builder) {
        builder.renamedFrom("oldAmount", "amount");
    }
}
```
