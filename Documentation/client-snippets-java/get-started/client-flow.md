```java title="Main.java"
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

class Main {
    void run() {
        var client = BlockingChronicleClient.connect(ChronicleOptions.development());
        var eventStore = client.getEventStore("ChronicleConsole");

        eventStore.getEventLog().append("some-event-source", new TestEvent("Hello world!"));
    }
}
```
