```kotlin
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.identity.Identity

class CorrelationIdentityCausationIdentity {
    fun forRequest(subject: String, name: String, userName: String): OperationContext =
        OperationContext.system().copy(causedBy = Identity(subject, name, userName))
}
```
