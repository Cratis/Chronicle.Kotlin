```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.migrations.EventTypeMigration;
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder;

@EventType(id = "dotnet-client-shipping-address-recorded", generation = 2)
record MigrationsDotnetClientCombineShippingAddressRecorded(String fullAddress) {
}

@EventType(id = "dotnet-client-shipping-address-recorded", generation = 1)
record MigrationsDotnetClientCombineShippingAddressRecordedV1(String street, String city) {
}

class MigrationsDotnetClientCombineShippingAddressRecordedMigration
        extends EventTypeMigration<MigrationsDotnetClientCombineShippingAddressRecorded,
                                   MigrationsDotnetClientCombineShippingAddressRecordedV1> {
    MigrationsDotnetClientCombineShippingAddressRecordedMigration() {
        super(MigrationsDotnetClientCombineShippingAddressRecorded.class,
              MigrationsDotnetClientCombineShippingAddressRecordedV1.class);
    }

    @Override
    public void upcast(EventTypeMigrationBuilder<MigrationsDotnetClientCombineShippingAddressRecorded,
                                                 MigrationsDotnetClientCombineShippingAddressRecordedV1> builder) {
        builder.combine("fullAddress", " ", "street", "city");
    }

    @Override
    public void downcast(EventTypeMigrationBuilder<MigrationsDotnetClientCombineShippingAddressRecordedV1,
                                                   MigrationsDotnetClientCombineShippingAddressRecorded> builder) {
        builder
            .split("street", "fullAddress", " ", 0)
            .split("city", "fullAddress", " ", 1);
    }
}
```
