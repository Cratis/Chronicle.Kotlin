```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.BatchContext;
import io.cratis.chronicle.observation.EventAndContext;
import io.cratis.chronicle.observation.EventBatch;
import io.cratis.chronicle.observation.Observes;
import io.cratis.chronicle.observation.Reducer;

@EventType
record BatchAccountOpened(String owner) {}

@EventType
record BatchMoneyDeposited(double amount) {}

@EventType
record BatchMoneyWithdrawn(double amount) {}

record BatchAccount(String owner, double balance) {}

@Reducer
class BatchAccountReducer {
    @Observes(BatchAccountOpened.class)
    @Observes(BatchMoneyDeposited.class)
    @Observes(BatchMoneyWithdrawn.class)
    public BatchAccount reduce(EventBatch events, BatchAccount current, BatchContext context) {
        BatchAccount account = current;
        for (EventAndContext e : events) {
            if (e.getEvent() instanceof BatchAccountOpened opened) {
                account = new BatchAccount(opened.owner(), 0);
            } else if (e.getEvent() instanceof BatchMoneyDeposited deposited && account != null) {
                account = new BatchAccount(account.owner(), account.balance() + deposited.amount());
            } else if (e.getEvent() instanceof BatchMoneyWithdrawn withdrawn && account != null) {
                account = new BatchAccount(account.owner(), account.balance() - withdrawn.amount());
            }
        }
        return account;
    }
}
```
