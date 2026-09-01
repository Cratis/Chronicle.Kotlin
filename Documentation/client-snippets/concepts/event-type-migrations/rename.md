```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "payment-processed", generation = 2)
data class MigrationsRenamePaymentProcessed(val amount: Double)

@EventType(id = "payment-processed", generation = 1)
data class MigrationsRenamePaymentProcessedV1(val oldAmount: Double)

class MigrationsRenamePaymentProcessedMigration :
    EventTypeMigration<MigrationsRenamePaymentProcessed, MigrationsRenamePaymentProcessedV1>(
        MigrationsRenamePaymentProcessed::class,
        MigrationsRenamePaymentProcessedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsRenamePaymentProcessed, MigrationsRenamePaymentProcessedV1>) {
        builder.renamedFrom(MigrationsRenamePaymentProcessed::amount, MigrationsRenamePaymentProcessedV1::oldAmount)
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsRenamePaymentProcessedV1, MigrationsRenamePaymentProcessed>) {
        builder.renamedFrom(MigrationsRenamePaymentProcessedV1::oldAmount, MigrationsRenamePaymentProcessed::amount)
    }
}
```
