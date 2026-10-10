```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

@EventType
record DepositMade(double amount) {}

@EventType
record WithdrawalMade(double amount) {}

@ReadModel
record AccountBalance(double balance, Instant lastUpdated) {
    AccountBalance() {
        this(0.0, Instant.EPOCH);
    }
}

@Reducer
class AccountBalanceReducer {
    AccountBalance deposited(DepositMade event, AccountBalance current, EventContext context) {
        double balance = current == null ? 0.0 : current.balance();
        return new AccountBalance(balance + event.amount(), context.getOccurred());
    }

    AccountBalance withdrawalMade(WithdrawalMade event, AccountBalance current, EventContext context) {
        double balance = current == null ? 0.0 : current.balance();
        return new AccountBalance(balance - event.amount(), context.getOccurred());
    }
}
```
