```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "shipping-address-recorded", generation = 2)
record MigrationsCombineShippingAddressRecorded(String formattedAddress) {
}

@EventType(id = "shipping-address-recorded", generation = 1)
record MigrationsCombineShippingAddressRecordedV1(String street, String city) {
}

class MigrationsCombineShippingAddressRecordedMigration
        extends EventTypeMigration<MigrationsCombineShippingAddressRecorded,
                                   MigrationsCombineShippingAddressRecordedV1> {
    MigrationsCombineShippingAddressRecordedMigration() {
        super(MigrationsCombineShippingAddressRecorded.class, MigrationsCombineShippingAddressRecordedV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsCombineShippingAddressRecorded,
                                                 MigrationsCombineShippingAddressRecordedV1> builder) {
        // Joins with space separator
        builder.combine("formattedAddress", " ", "street", "city");
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsCombineShippingAddressRecordedV1,
                                                   MigrationsCombineShippingAddressRecorded> builder) {
        builder
            .split("street", "formattedAddress", " ", 0)
            .split("city", "formattedAddress", " ", 1);
    }
}
```
