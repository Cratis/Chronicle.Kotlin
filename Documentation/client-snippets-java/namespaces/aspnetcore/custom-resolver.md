```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

// Supply the value from your application's configuration and declare this resolver as a Spring bean.
// The Chronicle Spring Boot starter uses an application-provided resolver instead of its built-in one.
class CustomNamespaceResolver implements IEventStoreNamespaceResolver {
    private final String tenantNamespace;

    public CustomNamespaceResolver(String tenantNamespace) {
        this.tenantNamespace = tenantNamespace;
    }

    @Override
    public String resolve() {
        return tenantNamespace == null ? "Default" : tenantNamespace;
    }
}
```
