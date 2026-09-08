```java
import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.identity.Identity;

class CorrelationIdentityCausationIdentity {
    OperationContext forRequest(String subject, String name, String userName) {
        return OperationContext.builder()
            .causedBy(new Identity(subject, name, userName, null))
            .build();
    }
}
```
