```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Takes the namespace from a header on the current HTTP request, falling back to the default
 * namespace for work happening outside a request. The Spring Boot integration ships this exact
 * strategy as io.cratis.chronicle.spring.namespaces.HttpHeaderNamespaceResolver, applied
 * automatically via cratis.chronicle.namespace-resolution.strategy=HTTP_HEADER - this version takes
 * the header lookup as a parameter so it has no framework dependency.
 */
class HttpHeaderNamespaceResolver(
    private val headerName: String,
    private val currentHeaderValue: (String) -> String?
) : IEventStoreNamespaceResolver {
    override fun resolve(): String =
        currentHeaderValue(headerName)?.takeIf { it.isNotBlank() } ?: EventStoreNamespaceName.default.value
}
```
