```java title="Choosing the graph sink"
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.sinks.WellKnownSinkTypes;

class GraphSinkOptions {
    // Neo4j is the default. The in-memory graph sink keeps the graph in the kernel's memory.
    ChronicleOptions configure() {
        return ChronicleOptions.development().withDefaultGraphSinkTypeId(WellKnownSinkTypes.IN_MEMORY_GRAPH);
    }
}
```
