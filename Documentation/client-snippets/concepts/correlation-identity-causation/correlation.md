```kotlin
import io.cratis.chronicle.OperationContext
import java.util.UUID

class CorrelationIdentityCausationCorrelation {
    fun newRequest(): OperationContext = OperationContext.system()

    fun continueRequest(context: OperationContext): OperationContext =
        context.copy(correlationId = context.correlationId)
}
```
