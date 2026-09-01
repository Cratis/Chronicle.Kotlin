```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Kotlin has no separate builder step to register a namespace resolver structurally - a resolver
 * is just an IEventStoreNamespaceResolver implementation, constructed with whatever configuration
 * it needs and handed to callers directly instead of wired up through a hosting builder.
 */
class ConfiguredNamespaceResolver(private val configuredNamespace: String?) : IEventStoreNamespaceResolver {
    override fun resolve(): String = configuredNamespace ?: EventStoreNamespaceName.default.value
}

fun createConfiguredResolver(tenantNamespace: String?): IEventStoreNamespaceResolver =
    ConfiguredNamespaceResolver(tenantNamespace)
```
