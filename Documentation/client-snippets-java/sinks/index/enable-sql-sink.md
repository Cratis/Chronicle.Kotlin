```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import io.cratis.chronicle.sinks.WellKnownSinkTypes;

class SinksIndexEnableSqlSink {
    ChronicleOptions configure() {
        return new ChronicleOptions(
            ChronicleConnectionString.Companion.parse("chronicle://localhost:35000"),
            "Unknown",
            WellKnownSinkTypes.SQL);
    }
}
```
