```java
import io.cratis.chronicle.IEventStore;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

class ReducersGettingStartedOrderService {
    private final IEventStore store;

    ReducersGettingStartedOrderService(IEventStore store) {
        this.store = store;
    }

    ReducersGettingStartedOrderSummary getOrderSummary(String orderId) throws InterruptedException {
        return (ReducersGettingStartedOrderSummary) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super ReducersGettingStartedOrderSummary>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(ReducersGettingStartedOrderSummary.class),
                    orderId,
                    readContinuation);
            });
    }
}
```
