```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.events.EventType;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

@EventType(id = "SchemaValidatedOrderPlaced")
record SchemaValidatedOrderPlaced(String customerId, double total) {}

class SchemaValidationExample {
    void append(IEventStore store, String eventSourceId, String customerId, double total) throws InterruptedException {
        var result = (AppendResult) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var appendContinuation = (Continuation<? super AppendResult>) continuation;
                return store.getEventLog().append(
                    eventSourceId,
                    new SchemaValidatedOrderPlaced(customerId, total),
                    null,
                    appendContinuation);
            });

        if (!result.isSuccess()) {
            result.getErrors().forEach(error ->
                System.out.println("Schema error: " + error));
        }
    }
}
```
