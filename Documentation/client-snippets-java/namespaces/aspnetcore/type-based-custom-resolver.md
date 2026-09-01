```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

// The Spring Boot integration steps aside for a resolver the application declares as its own bean
// (@Component, @Service, or an @Bean method) - annotating this class is enough for it to be
// discovered and used automatically, with no explicit wiring. This is the closest Java gets to
// setting EventStoreNamespaceResolverType and letting dependency injection resolve the type.
class TenantComponentNamespaceResolver implements IEventStoreNamespaceResolver {
    private final ITenantContext tenantContext;

    public TenantComponentNamespaceResolver(ITenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public String resolve() {
        String tenantId = tenantContext.getCurrentTenantId();
        return tenantId == null || tenantId.isBlank() ? "Default" : tenantId;
    }
}
```
