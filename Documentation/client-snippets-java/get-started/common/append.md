```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;
import java.util.UUID;

class GetStartedBookService {
    private final IEventStore eventStore;

    GetStartedBookService(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    String addBook() {
        var eventLog = eventStore.getEventLog();
        var bookId = UUID.randomUUID().toString();

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            @SuppressWarnings("unchecked")
            var appendContinuation = (Continuation<? super AppendResult>) continuation;
            return eventLog.append(
                bookId,
                new GetStartedBookAdded("The Pragmatic Programmer", "978-0135957059"),
                null,
                appendContinuation);
        });

        return bookId;
    }
}
```
