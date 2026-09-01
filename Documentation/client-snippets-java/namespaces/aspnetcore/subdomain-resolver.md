```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

import java.util.function.Supplier;

// Takes the namespace from the subdomain of the current request's host - "acme" in
// acme.example.com - falling back to the default namespace for a host with no subdomain, or for
// work happening outside a request. The Spring Boot integration ships this exact strategy as
// io.cratis.chronicle.spring.namespaces.SubdomainNamespaceResolver; this version takes the
// current host as a parameter so it has no framework dependency.
class SubdomainNamespaceResolver implements IEventStoreNamespaceResolver {
    private final Supplier<String> currentHost;

    public SubdomainNamespaceResolver(Supplier<String> currentHost) {
        this.currentHost = currentHost;
    }

    @Override
    public String resolve() {
        String host = currentHost.get();
        if (host == null) {
            return "Default";
        }
        String[] parts = host.split("\\.");
        return parts.length > 2 ? parts[0] : "Default";
    }
}
```
