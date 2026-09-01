```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

// The Spring Boot integration steps aside for a resolver the application declares as its own bean
// (@Component, @Service, or an @Bean method) - annotating this class is enough for it to be
// discovered and used automatically, with no explicit wiring. This is the closest Kotlin gets to
// setting EventStoreNamespaceResolverType and letting dependency injection resolve the type.
class TenantComponentNamespaceResolver(private val tenantContext: ITenantContext) : IEventStoreNamespaceResolver {
    override fun resolve(): String = tenantContext.currentTenantId.ifBlank { EventStoreNamespaceName.default.value }
}
```
