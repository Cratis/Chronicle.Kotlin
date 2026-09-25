```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingEventStore;
import io.cratis.chronicle.java.BlockingChronicleClient;

class ConfigurationIndexRegister {
    BlockingEventStore create() {
        BlockingChronicleClient client = BlockingChronicleClient.connect(ChronicleOptions.development());
        return client.getEventStore("my-store");
    }
}
```
