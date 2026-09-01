```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import io.cratis.chronicle.connection.ChronicleServerAddress;
import io.cratis.chronicle.java.BlockingChronicleClient;

import java.util.List;

// Java has no separate connection string builder type either — ChronicleConnectionString is a
// Kotlin data class, so its full constructor (with every property named) is called directly.
class ConnectionStringsFluentBuilder {
    BlockingChronicleClient create() {
        ChronicleConnectionString connectionString = new ChronicleConnectionString(
            List.of(new ChronicleServerAddress("server.example.com", 35000)),
            "clientId",
            "clientSecret",
            false,
            true,
            null,
            io.cratis.chronicle.connection.LoadBalancer.LEAST_CONNECTIONS,
            null,
            false);

        return BlockingChronicleClient.connect(new ChronicleOptions(connectionString));
    }
}
```
