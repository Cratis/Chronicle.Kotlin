```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.artifacts.IArtifactActivator;
import io.cratis.chronicle.artifacts.IClientArtifacts;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import io.cratis.chronicle.java.BlockingChronicleClient;

// Unlike the .NET client, structural dependencies are not separate constructor parameters on the
// client — they are carried on ChronicleOptions itself, which is where every named dependency
// below actually lives.
class StructuralDependenciesDirectClient {
    BlockingChronicleClient create(IClientArtifacts artifacts, IArtifactActivator artifactActivator) {
        ChronicleOptions options = new ChronicleOptions(
            ChronicleConnectionString.Companion.getDEVELOPMENT(),
            "Unknown",
            io.cratis.chronicle.sinks.WellKnownSinkTypes.MONGODB,
            true,
            artifacts,
            artifactActivator);
        return BlockingChronicleClient.connect(options);
    }
}
```
