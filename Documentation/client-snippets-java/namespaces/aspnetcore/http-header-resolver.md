```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

import java.util.function.Function;

// Takes the namespace from a header on the current HTTP request, falling back to the default
// namespace for work happening outside a request. The Spring Boot integration ships this exact
// strategy as io.cratis.chronicle.spring.namespaces.HttpHeaderNamespaceResolver, applied
// automatically via cratis.chronicle.namespace-resolution.strategy=HTTP_HEADER - this version
// takes the header lookup as a parameter so it has no framework dependency.
class HttpHeaderNamespaceResolver implements IEventStoreNamespaceResolver {
    private final String headerName;
    private final Function<String, String> currentHeaderValue;

    public HttpHeaderNamespaceResolver(String headerName, Function<String, String> currentHeaderValue) {
        this.headerName = headerName;
        this.currentHeaderValue = currentHeaderValue;
    }

    @Override
    public String resolve() {
        String value = currentHeaderValue.apply(headerName);
        return value == null || value.isBlank() ? "Default" : value;
    }
}
```
