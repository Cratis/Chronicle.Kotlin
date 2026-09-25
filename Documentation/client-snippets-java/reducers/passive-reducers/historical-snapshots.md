```java
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record PassiveReducersMoneyDeposited(double amount) {}

@ReadModel
record PassiveReducersAccountBalance(double balance) {
    PassiveReducersAccountBalance() {
        this(0.0);
    }
}

// Passive: nothing is stored; each read folds the account's events in the client.
@Reducer(isActive = false)
class PassiveReducersAccountBalanceReducer {
    public PassiveReducersAccountBalance deposited(
            PassiveReducersMoneyDeposited event, PassiveReducersAccountBalance state) {
        double current = state != null ? state.balance() : 0.0;
        return new PassiveReducersAccountBalance(current + event.amount());
    }
}

class PassiveReducersHistoricalBalanceService {
    private final BlockingEventStore eventStore;

    PassiveReducersHistoricalBalanceService(BlockingEventStore eventStore) {
        this.eventStore = eventStore;
    }

    // Returns the balance as of now. The JVM client has no read-as-of-a-date call; for past states,
    // read the snapshots with ReadModelsJavaBridge.getSnapshotsById and pick the one that occurred
    // before the date.
    PassiveReducersAccountBalance getCurrentBalance(String accountId) {
        return eventStore.getReadModels().getInstanceByKey(PassiveReducersAccountBalance.class, accountId);
    }
}
```
