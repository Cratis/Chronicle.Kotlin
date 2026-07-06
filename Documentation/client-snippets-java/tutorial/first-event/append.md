```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;
import java.util.UUID;

class TutorialFirstEventAppend {
    String addBook(IEventStore eventStore) throws InterruptedException {
        var bookId = UUID.randomUUID().toString();

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            @SuppressWarnings("unchecked")
            var appendContinuation = (Continuation<? super AppendResult>) continuation;
            return eventStore.getEventLog().append(
                bookId,
                new BookAdded("The Pragmatic Programmer", "978-0135957059"),
                null,
                appendContinuation);
        });

        return bookId;
    }
}
```
