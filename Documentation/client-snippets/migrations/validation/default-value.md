```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "migrations-validation-order-placed", generation = 1)
data class MigrationsValidationOrderPlacedV1(val orderId: String)

@EventType(id = "migrations-validation-order-placed", generation = 2)
data class MigrationsValidationOrderPlacedV2(val orderId: String, val currency: String)

class MigrationsValidationOrderPlacedMigration :
    EventTypeMigration<MigrationsValidationOrderPlacedV2, MigrationsValidationOrderPlacedV1>(
        MigrationsValidationOrderPlacedV2::class,
        MigrationsValidationOrderPlacedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsValidationOrderPlacedV2, MigrationsValidationOrderPlacedV1>) {
        builder.defaultValue(MigrationsValidationOrderPlacedV2::currency, "USD")
    }
}
```
