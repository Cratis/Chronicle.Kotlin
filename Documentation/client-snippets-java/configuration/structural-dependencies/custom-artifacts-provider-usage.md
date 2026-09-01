```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;

class StructuralDepsCustomArtifactsProviderUsage {
    ChronicleOptions create() {
        return new ChronicleOptions(
            ChronicleConnectionString.Companion.getDEVELOPMENT(),
            "Unknown",
            io.cratis.chronicle.sinks.WellKnownSinkTypes.MONGODB,
            true,
            new StructuralDepsMyArtifacts());
    }
}
```
