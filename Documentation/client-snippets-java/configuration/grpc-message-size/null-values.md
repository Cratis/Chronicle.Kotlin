```java
import io.cratis.chronicle.ChronicleOptions;

class MessageSizeGrpcDefaults {
    ChronicleOptions create() {
        return ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
            .withMaxReceiveMessageSize(null); // Uses gRPC default of 4 MB - not recommended
    }
}
```
