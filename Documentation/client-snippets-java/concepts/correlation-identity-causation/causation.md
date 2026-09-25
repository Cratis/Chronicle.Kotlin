```java
import io.cratis.chronicle.java.CausationManagerJavaBridge;

import java.util.Map;

import static io.cratis.chronicle.auditing.CausationManagerKt.getCausationManager;

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationCausation {
    void recordPlaceOrder(String orderId) {
        CausationManagerJavaBridge.add(
            getCausationManager(),
            "MyApp.Commands.PlaceOrder",
            Map.of("orderId", orderId));
    }
}
```
