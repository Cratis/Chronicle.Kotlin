```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

record TransactionalOrderPlaced(String orderId, double totalAmount) {}
record TransactionalInventoryReserved(String sku, int quantity) {}

class TransactionalOrderWorkflow {
    void commitOrder(IEventStore store) {
        var unitOfWork = store.getUnitOfWorkManager().begin();

        try {
            BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> {
                    @SuppressWarnings("unchecked")
                    var appendContinuation = (Continuation<? super AppendResult>) continuation;
                    return store.getEventLog().getTransactional().append(
                        "order-123",
                        new TransactionalOrderPlaced("order-123", 99.95),
                        null,
                        appendContinuation);
                });

            BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> {
                    @SuppressWarnings("unchecked")
                    var appendContinuation = (Continuation<? super AppendResult>) continuation;
                    return store.getEventLog().getTransactional().append(
                        "inventory-widget",
                        new TransactionalInventoryReserved("widget", 1),
                        null,
                        appendContinuation);
                });

            BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> {
                    @SuppressWarnings("unchecked")
                    var commitContinuation = (Continuation<? super Unit>) continuation;
                    return unitOfWork.commit(commitContinuation);
                });
        } catch (RuntimeException exception) {
            BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> {
                    @SuppressWarnings("unchecked")
                    var rollbackContinuation = (Continuation<? super Unit>) continuation;
                    return unitOfWork.rollback(rollbackContinuation);
                });
            throw exception;
        }
    }
}
```
