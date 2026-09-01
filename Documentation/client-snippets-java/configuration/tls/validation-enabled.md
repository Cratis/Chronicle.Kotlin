```java
import io.cratis.chronicle.ChronicleOptions;

class ConfigurationTlsValidationEnabled {
    ChronicleOptions create() {
        return ChronicleOptions.fromConnectionString("chronicle://my-server:35000?skipTlsValidation=false");
    }
}
```
