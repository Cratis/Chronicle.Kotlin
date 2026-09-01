```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingChronicleClient;

class NamespacesDotNetClientTenantResolverUsage {
    IEventStore create(BlockingChronicleClient client, String name, ITenantContext tenantContext) {
        String namespace = new TenantNamespaceResolver(tenantContext).resolve();
        return client.getEventStore(name, namespace).unwrap();
    }
}
```
