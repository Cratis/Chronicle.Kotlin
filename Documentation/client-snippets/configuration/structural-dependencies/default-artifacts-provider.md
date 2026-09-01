```kotlin
import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.IClientArtifacts

// Scans only the given packages and everything beneath them, instead of the whole classpath.
fun scopedArtifacts(): IClientArtifacts = ClientArtifacts("com.acme.ordering", "com.acme.shipping")

// The classpath-wide instance used when no artifacts are configured, shared across every event
// store in the process so the classpath is scanned at most once.
fun defaultArtifacts(): IClientArtifacts = ClientArtifacts.default
```
