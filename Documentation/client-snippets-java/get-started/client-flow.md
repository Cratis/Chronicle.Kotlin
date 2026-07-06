```java title="Main.java"
import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

class Main {
    void run() throws InterruptedException {
        var client = new ChronicleClient(ChronicleOptions.Companion.development());
        var eventStore = client.getEventStore("ChronicleConsole", "Default");

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
            @SuppressWarnings("unchecked")
            var appendContinuation = (Continuation<? super AppendResult>) continuation;
            return eventStore.getEventLog().append(
                "some-event-source",
                new TestEvent("Hello world!"),
                null,
                appendContinuation);
        });
    }
}
```
