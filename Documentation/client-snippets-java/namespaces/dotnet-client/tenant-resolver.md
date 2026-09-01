```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

interface ITenantContext {
    String getCurrentTenantId();
}

class TenantNamespaceResolver implements IEventStoreNamespaceResolver {
    private final ITenantContext tenantContext;

    public TenantNamespaceResolver(ITenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public String resolve() {
        return tenantContext.getCurrentTenantId();
    }
}
```
