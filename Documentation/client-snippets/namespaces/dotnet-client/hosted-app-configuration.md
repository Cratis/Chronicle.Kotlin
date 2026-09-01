```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Kotlin has no separate hosted-app builder to configure a resolver on structurally - a worker
 * service resolves the namespace and asks the client for that event store exactly like a console
 * application would.
 */
fun currentEventStore(client: ChronicleClient, name: String, resolver: IEventStoreNamespaceResolver): IEventStore =
    client.getEventStore(name, resolver.resolve())
```
