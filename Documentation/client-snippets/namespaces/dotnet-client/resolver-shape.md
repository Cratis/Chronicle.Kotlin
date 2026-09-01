```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

class NamespacesDotNetClientSampleResolver : IEventStoreNamespaceResolver {
    override fun resolve(): String = EventStoreNamespaceName.default.value
}
```
