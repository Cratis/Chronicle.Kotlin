```java
import io.cratis.chronicle.artifacts.ClientArtifacts;
import io.cratis.chronicle.artifacts.IClientArtifacts;

class StructuralDepsDefaultArtifactsProvider {
    // Scans only the given packages and everything beneath them, instead of the whole classpath.
    ClientArtifacts create() {
        return new ClientArtifacts("com.acme.ordering", "com.acme.shipping");
    }

    // The classpath-wide instance used when no artifacts are configured, shared across every
    // event store in the process so the classpath is scanned at most once.
    IClientArtifacts getDefault() {
        return ClientArtifacts.Companion.getDefault();
    }
}
```
