```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;
import java.time.Instant;

@EventType(id = "reducers-index-deposit-made")
record ReducersIndexDepositMade(double amount) {}

@EventType(id = "reducers-index-withdrawal-made")
record ReducersIndexWithdrawalMade(double amount) {}

@ReadModel
record ReducersIndexAccountBalance(double balance, Instant lastUpdated) {
    ReducersIndexAccountBalance() {
        this(0.0, Instant.EPOCH);
    }
}

@Reducer
class ReducersIndexAccountBalanceReducer {
    ReducersIndexAccountBalance deposited(ReducersIndexDepositMade event, ReducersIndexAccountBalance current) {
        double currentBalance = current != null ? current.balance() : 0.0;
        return new ReducersIndexAccountBalance(currentBalance + event.amount(), Instant.now());
    }

    ReducersIndexAccountBalance withdrawalMade(ReducersIndexWithdrawalMade event, ReducersIndexAccountBalance current) {
        double currentBalance = current != null ? current.balance() : 0.0;
        return new ReducersIndexAccountBalance(currentBalance - event.amount(), Instant.now());
    }
}
```
