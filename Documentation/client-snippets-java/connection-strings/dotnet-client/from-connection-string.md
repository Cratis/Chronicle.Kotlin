```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

class ConnectionStringsFromConnectionString {
    BlockingChronicleClient create() {
        ChronicleOptions options = ChronicleOptions.fromConnectionString("chronicle://localhost:35000");
        return BlockingChronicleClient.connect(options);
    }
}
```
