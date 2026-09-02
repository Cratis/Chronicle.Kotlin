```java
import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.auditing.Causation;
import java.time.Instant;
import java.util.Map;

class CorrelationIdentityCausationCausation {
    OperationContext recordPlaceOrder(OperationContext context, String orderId) {
        return OperationContext.builder(context)
            .causation(Causation.of(
                Instant.now(), "MyApp.Commands.PlaceOrder", Map.of("orderId", orderId)))
            .build();
    }
}
```
