```kotlin title="Choosing the graph sink"
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.sinks.WellKnownSinkTypes

// Neo4j is the default. The in-memory graph sink keeps the graph in the kernel's memory.
fun optionsWithInMemoryGraphSink(): ChronicleOptions =
    ChronicleOptions.development().withDefaultGraphSinkTypeId(WellKnownSinkTypes.IN_MEMORY_GRAPH)
```
