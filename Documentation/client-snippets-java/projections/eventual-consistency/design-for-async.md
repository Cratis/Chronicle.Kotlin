```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;
import java.util.UUID;

@EventType
record EcBookCreated(String title, String author) {}

record EcBookInventory(String id, String title, String author) {
    EcBookInventory() {
        this("", "", "");
    }
}

class EcBookService {
    private final IEventStore store;

    EcBookService(IEventStore store) {
        this.store = store;
    }

    // Good — fire and forget: don't wait for the projection before returning
    String createBook(String title, String author) throws InterruptedException {
        var bookId = UUID.randomUUID().toString();
        var eventLog = store.getEventLog();

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            @SuppressWarnings("unchecked")
            var appendContinuation = (Continuation<Object>) continuation;
            return eventLog.append(bookId, new EcBookCreated(title, author), null, appendContinuation);
        });

        return bookId;
    }

    // Problematic — expecting immediate consistency
    EcBookInventory createBookAndReturn(String title, String author) throws InterruptedException {
        var bookId = createBook(title, author);

        // The projection may not have run yet — this can return null or a stale instance
        return (EcBookInventory) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super EcBookInventory>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(EcBookInventory.class),
                    bookId,
                    readContinuation);
            });
    }
}
```
