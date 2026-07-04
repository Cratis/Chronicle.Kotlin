```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

@EventType
record EcCqsBookCreated(String title) {}

record EcCqsBook(String id, String title) {
    EcCqsBook() {
        this("", "");
    }
}

// Commands — fire and forget, never return projected state
class EcCqsBookCommandHandler {
    private final IEventStore store;

    EcCqsBookCommandHandler(IEventStore store) {
        this.store = store;
    }

    void create(String bookId, String title) {
        var eventLog = store.getEventLog();

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            @SuppressWarnings("unchecked")
            var appendContinuation = (Continuation<Object>) continuation;
            return eventLog.append(bookId, new EcCqsBookCreated(title), null, appendContinuation);
        });
    }
}

// Queries — always read from projections
class EcCqsBookQueryHandler {
    private final IEventStore store;

    EcCqsBookQueryHandler(IEventStore store) {
        this.store = store;
    }

    EcCqsBook getBook(String bookId) {
        return (EcCqsBook) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super EcCqsBook>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(EcCqsBook.class),
                    bookId,
                    readContinuation);
            });
    }
}
```
