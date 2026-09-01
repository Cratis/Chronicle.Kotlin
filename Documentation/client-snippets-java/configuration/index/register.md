```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingChronicleClient;

class ConfigurationIndexRegister {
    IEventStore create() {
        BlockingChronicleClient client = BlockingChronicleClient.connect(ChronicleOptions.development());
        return client.getEventStore("my-store").unwrap();
    }
}
```
