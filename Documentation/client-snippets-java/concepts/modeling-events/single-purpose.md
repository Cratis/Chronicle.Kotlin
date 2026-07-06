```java
import io.cratis.chronicle.events.EventType;

record ModelingEventsCustomerName(String value) {}
record ModelingEventsEmail(String value) {}
record ModelingEventsDeactivationReason(String value) {}
record ModelingEventsCustomerAddress(String street, String city) {}

// One event trying to be everything — consumers must guess what changed
@EventType(id = "modeling-events-customer-updated")
record ModelingEventsCustomerUpdated(
    ModelingEventsCustomerName name,
    ModelingEventsCustomerAddress address,
    ModelingEventsEmail email,
    Boolean deactivated) {}

// Distinct facts — each consumer subscribes to exactly what it cares about
@EventType(id = "modeling-events-customer-renamed")
record ModelingEventsCustomerRenamed(ModelingEventsCustomerName name) {}

@EventType(id = "modeling-events-customer-address-changed")
record ModelingEventsCustomerAddressChanged(ModelingEventsCustomerAddress address) {}

@EventType(id = "modeling-events-customer-deactivated")
record ModelingEventsCustomerDeactivated(ModelingEventsDeactivationReason reason) {}
```
