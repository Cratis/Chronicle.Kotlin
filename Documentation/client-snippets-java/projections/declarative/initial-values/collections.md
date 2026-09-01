```java title="Initialize collections"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.Collections;
import java.util.List;

class InitialValuesAddress {
    public String street;
    public String city;

    InitialValuesAddress(String street, String city) {
        this.street = street;
        this.city = city;
    }
}

@EventType(id = "initial-values-customer-registered")
record InitialValuesCustomerRegistered(String name) {}

class InitialValuesCustomerRecord {
    public String name = "";
    public List<InitialValuesAddress> addresses = Collections.emptyList();
    public List<String> tags = Collections.emptyList();
}

class InitialValuesCustomerRecordProjection implements IProjectionFor<InitialValuesCustomerRecord> {
    @Override
    public void define(IProjectionBuilderFor<InitialValuesCustomerRecord> builder) {
        builder.from(InitialValuesCustomerRegistered.class);
    }
}
```
