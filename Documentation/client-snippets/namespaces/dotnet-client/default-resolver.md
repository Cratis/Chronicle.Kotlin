```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.namespaces.DefaultEventStoreNamespaceResolver

fun getDefaultEventStore(client: ChronicleClient, name: String): IEventStore =
    client.getEventStore(name, DefaultEventStoreNamespaceResolver.resolve())
```
