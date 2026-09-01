```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.namespaces.DefaultEventStoreNamespaceResolver;

class NamespacesDotNetClientDefaultResolver {
    IEventStore create(BlockingChronicleClient client, String name) {
        String namespace = DefaultEventStoreNamespaceResolver.INSTANCE.resolve();
        return client.getEventStore(name, namespace).unwrap();
    }
}
```
