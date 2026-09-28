```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

// Supply the value from your application's configuration and declare this resolver as a Spring bean.
// The Chronicle Spring Boot starter uses an application-provided resolver instead of its built-in one.
class CustomNamespaceResolver(private val tenantNamespace: String?) : IEventStoreNamespaceResolver {
    override fun resolve(): String = tenantNamespace ?: EventStoreNamespaceName.default.value
}
```
