```java
import io.cratis.chronicle.OperationContext;

class CorrelationIdentityCausationCorrelation {
    OperationContext newRequest() {
        return OperationContext.system();
    }
}
```
