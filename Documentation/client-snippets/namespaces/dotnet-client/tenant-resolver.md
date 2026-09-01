```kotlin
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

interface ITenantContext {
    val currentTenantId: String
}

class TenantNamespaceResolver(private val tenantContext: ITenantContext) : IEventStoreNamespaceResolver {
    override fun resolve(): String = tenantContext.currentTenantId
}
```
