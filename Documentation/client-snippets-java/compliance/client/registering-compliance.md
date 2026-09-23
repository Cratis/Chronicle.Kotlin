```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.java.BlockingEventStore;

// The Java client has no separate compliance registration step. Every event type and read model
// it registers carries its @Pii metadata in the schema it sends to the kernel, so connecting the
// client is all compliance needs - and the compliance service is on every event store it returns.
class ComplianceClientRegistration {
    static BlockingEventStore configure() {
        BlockingChronicleClient client = BlockingChronicleClient.connect(ChronicleOptions.development());
        return client.getEventStore("Sales");
    }
}
```
