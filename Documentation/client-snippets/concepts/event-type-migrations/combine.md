```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "shipping-address-recorded", generation = 2)
data class MigrationsCombineShippingAddressRecorded(val formattedAddress: String)

@EventType(id = "shipping-address-recorded", generation = 1)
data class MigrationsCombineShippingAddressRecordedV1(val street: String, val city: String)

class MigrationsCombineShippingAddressRecordedMigration :
    EventTypeMigration<MigrationsCombineShippingAddressRecorded, MigrationsCombineShippingAddressRecordedV1>(
        MigrationsCombineShippingAddressRecorded::class,
        MigrationsCombineShippingAddressRecordedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsCombineShippingAddressRecorded, MigrationsCombineShippingAddressRecordedV1>) {
        // Joins with space separator
        builder.combine(
            MigrationsCombineShippingAddressRecorded::formattedAddress,
            " ",
            MigrationsCombineShippingAddressRecordedV1::street,
            MigrationsCombineShippingAddressRecordedV1::city
        )
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsCombineShippingAddressRecordedV1, MigrationsCombineShippingAddressRecorded>) {
        builder
            .split(MigrationsCombineShippingAddressRecordedV1::street, MigrationsCombineShippingAddressRecorded::formattedAddress, " ", 0)
            .split(MigrationsCombineShippingAddressRecordedV1::city, MigrationsCombineShippingAddressRecorded::formattedAddress, " ", 1)
    }
}
```
