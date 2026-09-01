```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;

class ConfigurationChronicleOptionsConnectionString {
    ChronicleOptions create() {
        return new ChronicleOptions(ChronicleConnectionString.Companion.parse("chronicle://myserver:35000"));
    }
}
```
