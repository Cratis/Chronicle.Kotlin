```java
import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;

class ConsoleConnect {
    void run() {
        // ChronicleOptions.development() points at the local dev kernel on chronicle://localhost:35000
        var client = new ChronicleClient(ChronicleOptions.Companion.development());
        var eventStore = client.getEventStore("Quickstart", "Default");
        System.out.println("Connected to event store: " + eventStore.getName());

        // Use eventStore for the lifetime of your program — appending, querying, and so on.
    }
}
```
