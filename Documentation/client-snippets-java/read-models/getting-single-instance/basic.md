```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record AccountInfo(String name, double balance) {
    AccountInfo() {
        this("", 0.0);
    }
}

class ReadModelLookup {
    void printAccount(IEventStore store, String accountId) {
        var account = new BlockingEventStore(store)
            .getReadModels()
            .getInstanceByKey(AccountInfo.class, accountId);

        if (account != null) {
            System.out.println(account.name() + ": " + account.balance());
        }
    }
}
```
