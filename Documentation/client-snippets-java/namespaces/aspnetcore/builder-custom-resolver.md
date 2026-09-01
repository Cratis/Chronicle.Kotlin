```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

// Java has no separate builder step to register a namespace resolver structurally - a resolver
// is just an IEventStoreNamespaceResolver implementation, constructed with whatever configuration
// it needs and handed to callers directly instead of wired up through a hosting builder.
class ConfiguredNamespaceResolver implements IEventStoreNamespaceResolver {
    private final String configuredNamespace;

    public ConfiguredNamespaceResolver(String configuredNamespace) {
        this.configuredNamespace = configuredNamespace;
    }

    @Override
    public String resolve() {
        return configuredNamespace == null ? "Default" : configuredNamespace;
    }

    public static IEventStoreNamespaceResolver createConfiguredResolver(String tenantNamespace) {
        return new ConfiguredNamespaceResolver(tenantNamespace);
    }
}
```
