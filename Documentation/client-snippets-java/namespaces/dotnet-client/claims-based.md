```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

import java.util.Map;
import java.util.function.Supplier;

// Takes the namespace from a claim on the currently authenticated principal, falling back to the
// default namespace for anonymous requests or work happening outside a request. The Spring Boot
// integration ships this exact strategy as
// io.cratis.chronicle.spring.namespaces.AuthenticationNamespaceResolver; this version takes the
// current claims as a parameter so it has no framework dependency.
class ClaimsBasedNamespaceResolver implements IEventStoreNamespaceResolver {
    private final String claim;
    private final Supplier<Map<String, String>> currentClaims;

    public ClaimsBasedNamespaceResolver(String claim, Supplier<Map<String, String>> currentClaims) {
        this.claim = claim;
        this.currentClaims = currentClaims;
    }

    @Override
    public String resolve() {
        Map<String, String> claims = currentClaims.get();
        String value = claims == null ? null : claims.get(claim);
        return value == null ? "Default" : value;
    }

    public static IEventStoreNamespaceResolver createWithDefaultClaimType(Supplier<Map<String, String>> currentClaims) {
        return new ClaimsBasedNamespaceResolver("tenant_id", currentClaims);
    }

    public static IEventStoreNamespaceResolver createWithCustomClaimType(Supplier<Map<String, String>> currentClaims) {
        return new ClaimsBasedNamespaceResolver("custom_tenant_claim", currentClaims);
    }
}
```
