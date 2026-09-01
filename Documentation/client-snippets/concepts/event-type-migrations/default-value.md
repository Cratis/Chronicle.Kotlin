```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "order-shipped", generation = 2)
data class MigrationsDefaultValueOrderShipped(val trackingNumber: String, val retryCount: Int, val description: String)

@EventType(id = "order-shipped", generation = 1)
data class MigrationsDefaultValueOrderShippedV1(val trackingNumber: String)

class MigrationsDefaultValueOrderShippedMigration :
    EventTypeMigration<MigrationsDefaultValueOrderShipped, MigrationsDefaultValueOrderShippedV1>(
        MigrationsDefaultValueOrderShipped::class,
        MigrationsDefaultValueOrderShippedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDefaultValueOrderShipped, MigrationsDefaultValueOrderShippedV1>) {
        builder
            .defaultValue(MigrationsDefaultValueOrderShipped::retryCount, 42)
            .defaultValue(MigrationsDefaultValueOrderShipped::description, "default string")
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDefaultValueOrderShippedV1, MigrationsDefaultValueOrderShipped>) {
        // retryCount and description did not exist in generation 1 — nothing to map back
    }
}
```
