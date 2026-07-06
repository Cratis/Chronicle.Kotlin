```java
import io.cratis.chronicle.IEventStore;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

record DesigningReadModelsCustomerDetail(String id, String name) {}

class DesigningReadModelsCustomerDetailService {
    private final IEventStore store;

    DesigningReadModelsCustomerDetailService(IEventStore store) {
        this.store = store;
    }

    DesigningReadModelsCustomerDetail getDetail(String customerId) throws InterruptedException {
        return (DesigningReadModelsCustomerDetail) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super DesigningReadModelsCustomerDetail>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(DesigningReadModelsCustomerDetail.class),
                    customerId,
                    readContinuation);
            });
    }
}
```
