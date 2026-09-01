```java
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

class NamespacesDotNetClientSampleResolver implements IEventStoreNamespaceResolver {
    @Override
    public String resolve() {
        return "Default";
    }
}
```
