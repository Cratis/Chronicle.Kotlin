```java
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.java.BlockingEventStore;
import io.cratis.chronicle.java.ComplianceServiceJavaBridge;

class ComplianceErasureDeleteKey {
    static void delete(BlockingChronicleClient chronicleClient) {
        BlockingEventStore eventStore = chronicleClient.getEventStore("Sales");
        ComplianceServiceJavaBridge.deleteEncryptionKey(eventStore.unwrap().getCompliance(), "person-42");
    }
}
```
