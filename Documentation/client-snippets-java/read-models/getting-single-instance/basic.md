```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.readModels.ReadModel;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.Continuation;

@ReadModel
record AccountInfo(String name, double balance) {
    AccountInfo() {
        this("", 0.0);
    }
}

class ReadModelLookup {
    void printAccount(IEventStore store, String accountId) throws InterruptedException {
        var account = (AccountInfo) BuildersKt.runBlocking(
            EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> {
                @SuppressWarnings("unchecked")
                var readContinuation = (Continuation<? super AccountInfo>) continuation;
                return store.getReadModels().getInstanceByKey(
                    JvmClassMappingKt.getKotlinClass(AccountInfo.class),
                    accountId,
                    readContinuation);
            });

        if (account != null) {
            System.out.println(account.name() + ": " + account.balance());
        }
    }
}
```
