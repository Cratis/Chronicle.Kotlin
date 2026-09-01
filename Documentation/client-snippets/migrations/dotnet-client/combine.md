```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

@EventType(id = "dotnet-client-shipping-address-recorded", generation = 2)
data class MigrationsDotnetClientCombineShippingAddressRecorded(val fullAddress: String)

@EventType(id = "dotnet-client-shipping-address-recorded", generation = 1)
data class MigrationsDotnetClientCombineShippingAddressRecordedV1(val street: String, val city: String)

class MigrationsDotnetClientCombineShippingAddressRecordedMigration :
    EventTypeMigration<MigrationsDotnetClientCombineShippingAddressRecorded, MigrationsDotnetClientCombineShippingAddressRecordedV1>(
        MigrationsDotnetClientCombineShippingAddressRecorded::class,
        MigrationsDotnetClientCombineShippingAddressRecordedV1::class
    ) {
    override fun upcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientCombineShippingAddressRecorded, MigrationsDotnetClientCombineShippingAddressRecordedV1>) {
        builder.combine(
            MigrationsDotnetClientCombineShippingAddressRecorded::fullAddress,
            " ",
            MigrationsDotnetClientCombineShippingAddressRecordedV1::street,
            MigrationsDotnetClientCombineShippingAddressRecordedV1::city
        )
    }

    override fun downcast(builder: EventTypeMigrationBuilder<MigrationsDotnetClientCombineShippingAddressRecordedV1, MigrationsDotnetClientCombineShippingAddressRecorded>) {
        builder
            .split(MigrationsDotnetClientCombineShippingAddressRecordedV1::street, MigrationsDotnetClientCombineShippingAddressRecorded::fullAddress, " ", 0)
            .split(MigrationsDotnetClientCombineShippingAddressRecordedV1::city, MigrationsDotnetClientCombineShippingAddressRecorded::fullAddress, " ", 1)
    }
}
```
