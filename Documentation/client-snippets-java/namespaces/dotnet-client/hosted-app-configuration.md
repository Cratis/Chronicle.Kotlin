```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

// Java has no separate hosted-app builder to configure a resolver on structurally - a worker
// service resolves the namespace and asks the client for that event store exactly like a console
// application would.
class NamespacesDotNetClientHostedAppConfiguration {
    IEventStore currentEventStore(BlockingChronicleClient client, String name, IEventStoreNamespaceResolver resolver) {
        return client.getEventStore(name, resolver.resolve()).unwrap();
    }
}
```
