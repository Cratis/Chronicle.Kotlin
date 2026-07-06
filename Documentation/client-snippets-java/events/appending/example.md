```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.events.EventType;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

@EventType(id = "OrderPlaced")
record OrderPlaced(String customerId, double total) {}

class CheckoutService {
    private final IEventStore store;

    CheckoutService(IEventStore store) {
        this.store = store;
    }

    void placeOrder(String orderId, String customerId, double total) throws InterruptedException {
        var result = (AppendResult) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var appendContinuation = (Continuation<? super AppendResult>) continuation;
                return store.getEventLog().append(
                    orderId,
                    new OrderPlaced(customerId, total),
                    null,
                    appendContinuation);
            });

        if (!result.isSuccess()) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
