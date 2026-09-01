```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import io.cratis.chronicle.java.BlockingChronicleClient;

class ConnectionStringsDevelopmentDefaultsEquivalent {
    BlockingChronicleClient createFromOptions() {
        return BlockingChronicleClient.connect(ChronicleOptions.development());
    }

    BlockingChronicleClient createFromConnectionString() {
        return BlockingChronicleClient.connect(new ChronicleOptions(ChronicleConnectionString.Companion.getDEVELOPMENT()));
    }
}
```
