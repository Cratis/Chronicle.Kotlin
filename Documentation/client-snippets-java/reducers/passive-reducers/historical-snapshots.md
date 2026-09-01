```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record PassiveReducersAccountBalance(double balance) {
    PassiveReducersAccountBalance() {
        this(0.0);
    }
}

class PassiveReducersHistoricalBalanceService {
    private final EventStore eventStore;

    PassiveReducersHistoricalBalanceService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    // Passive reducer computes state on-demand from historical events
    PassiveReducersAccountBalance getBalanceAtDate(String accountId) {
        return ReadModelsJavaBridge.getInstanceByKey(
            eventStore.getReadModels(), PassiveReducersAccountBalance.class, accountId);
    }
}
```
