```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

// Java has no appsettings.json-style configuration binding — read the connection string from
// wherever the application keeps its configuration (here, an environment variable) and pass it
// straight to the client.
class ConnectionStringsConfigurationRegister {
    BlockingChronicleClient create() {
        String connectionString = System.getenv("CHRONICLE_CONNECTION_STRING");
        if (connectionString == null) {
            connectionString = "chronicle://localhost:35000";
        }
        return BlockingChronicleClient.connect(ChronicleOptions.fromConnectionString(connectionString));
    }
}
```
