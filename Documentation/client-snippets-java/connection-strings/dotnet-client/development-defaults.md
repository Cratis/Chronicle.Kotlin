```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

class ConnectionStringsDevelopmentDefaults {
    BlockingChronicleClient create() {
        ChronicleOptions options = ChronicleOptions.development();
        return BlockingChronicleClient.connect(options);
    }
}
```
