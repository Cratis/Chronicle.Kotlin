```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import io.cratis.chronicle.sinks.WellKnownSinkTypes;

class ConfigurationChronicleOptionsSqlSink {
    ChronicleOptions create() {
        return new ChronicleOptions(ChronicleConnectionString.Companion.getDEVELOPMENT(), "Unknown", WellKnownSinkTypes.SQL);
    }
}
```
