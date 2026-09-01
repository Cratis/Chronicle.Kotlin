```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.IEventStore

fun getTenantEventStore(client: ChronicleClient, name: String, tenantContext: ITenantContext): IEventStore =
    client.getEventStore(name, TenantNamespaceResolver(tenantContext).resolve())
```
