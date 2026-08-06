```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType(id = "subject-shipping-address-changed")
record SubjectShippingAddressChanged(String street) {}

class SubjectShippingService {
    private final IEventStore eventStore;

    SubjectShippingService(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void changeAddress(String orderId, String customerId, String street) {
        // The event happens to the order, but the address is the customer's data - so the
        // subject is the customer, not the event source.
        EventLogJavaBridge.append(
            eventStore.getEventLog(),
            orderId,
            new SubjectShippingAddressChanged(street),
            new AppendOptionsBuilder().subject(customerId).build());
    }
}
```
