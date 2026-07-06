```java
import io.cratis.chronicle.IEventStore;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Job;

class ReactorRegistration {
    private final ReactorEmailGateway emailGateway;

    ReactorRegistration(ReactorEmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    Job register(IEventStore store) throws InterruptedException {
        return (Job) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var registerContinuation = (Continuation<? super Job>) continuation;
                return store.getReactors().register(
                    new OrderNotificationsReactor(emailGateway),
                    registerContinuation);
            });
    }
}
```
