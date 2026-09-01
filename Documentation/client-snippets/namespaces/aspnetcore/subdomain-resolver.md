```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Takes the namespace from the subdomain of the current request's host - "acme" in
 * acme.example.com - falling back to the default namespace for a host with no subdomain, or for
 * work happening outside a request. The Spring Boot integration ships this exact strategy as
 * io.cratis.chronicle.spring.namespaces.SubdomainNamespaceResolver; this version takes the current
 * host as a parameter so it has no framework dependency.
 */
class SubdomainNamespaceResolver(private val currentHost: () -> String?) : IEventStoreNamespaceResolver {
    override fun resolve(): String {
        val parts = currentHost()?.split('.') ?: return EventStoreNamespaceName.default.value
        return if (parts.size > 2) parts.first() else EventStoreNamespaceName.default.value
    }
}
```
