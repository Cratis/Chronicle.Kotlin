```java
import io.cratis.chronicle.events.EventType;

record ModelingEventsCustomerName(String value) {}
record ModelingEventsEmail(String value) {}
record ModelingEventsDeactivationReason(String value) {}
record ModelingEventsCustomerAddress(String street, String city) {}

// One event trying to be everything — consumers must guess what changed
@EventType
record ModelingEventsCustomerUpdated(
    ModelingEventsCustomerName name,
    ModelingEventsCustomerAddress address,
    ModelingEventsEmail email,
    Boolean deactivated) {}

// Distinct facts — each consumer subscribes to exactly what it cares about
@EventType
record ModelingEventsCustomerRenamed(ModelingEventsCustomerName name) {}

@EventType
record ModelingEventsCustomerAddressChanged(ModelingEventsCustomerAddress address) {}

@EventType
record ModelingEventsCustomerDeactivated(ModelingEventsDeactivationReason reason) {}
```
