```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.math.BigDecimal;
import java.time.Instant;

@EventType
public record DepositMade(BigDecimal amount) {}

@EventType
public record WithdrawalMade(BigDecimal amount) {}

@ReadModel
public record AccountBalance(BigDecimal balance, Instant lastUpdated) {
    public AccountBalance() {
        this(BigDecimal.ZERO, Instant.EPOCH);
    }
}

@Reducer
public class AccountBalanceReducer {
    public AccountBalance deposited(DepositMade event, AccountBalance current, EventContext context) {
        BigDecimal balance = current == null ? BigDecimal.ZERO : current.balance();
        return new AccountBalance(balance.add(event.amount()), context.getOccurred());
    }

    public AccountBalance withdrawalMade(WithdrawalMade event, AccountBalance current, EventContext context) {
        BigDecimal balance = current == null ? BigDecimal.ZERO : current.balance();
        return new AccountBalance(balance.subtract(event.amount()), context.getOccurred());
    }
}
```
