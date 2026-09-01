```java
import io.cratis.chronicle.java.CausationManagerJavaBridge;

import java.util.Map;

import static io.cratis.chronicle.auditing.CausationManagerKt.getCausationManager;

class CorrelationIdentityCausationCausation {
    void recordPlaceOrder(String orderId) {
        CausationManagerJavaBridge.add(
            getCausationManager(),
            "MyApp.Commands.PlaceOrder",
            Map.of("orderId", orderId));
    }
}
```
