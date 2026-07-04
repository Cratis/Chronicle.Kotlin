```java
import io.cratis.chronicle.IEventStore;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

record ScenariosQueryBook(String title, boolean onLoan) {}

class ScenariosQueryBookService {
    private final IEventStore store;

    ScenariosQueryBookService(IEventStore store) {
        this.store = store;
    }

    ScenariosQueryBook getBook(String bookId) {
        return (ScenariosQueryBook) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super ScenariosQueryBook>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(ScenariosQueryBook.class),
                    bookId,
                    readContinuation);
            });
    }
}
```
