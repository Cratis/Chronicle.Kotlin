```java
import io.cratis.chronicle.ChronicleOptions;

class TlsConnectionStringSkipValidation {
    ChronicleOptions create() {
        return ChronicleOptions.fromConnectionString("chronicle://localhost:35000?skipTlsValidation=true");
    }
}
```
