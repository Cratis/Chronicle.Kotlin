```java
import io.cratis.chronicle.ChronicleOptions;

class MessageSizeConfiguration {
    ChronicleOptions create() {
        return ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
            .withMaxReceiveMessageSize(200 * 1024 * 1024); // 200 MB
    }
}
```
