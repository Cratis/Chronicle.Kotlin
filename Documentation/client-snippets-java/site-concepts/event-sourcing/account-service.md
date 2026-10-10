```java
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.EventLogJavaBridge;

import java.math.BigDecimal;

public class AccountService {
    private final IEventLog eventLog;

    public AccountService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    public void withdraw(String accountId, BigDecimal amount) {
        var result = EventLogJavaBridge.append(eventLog, accountId, new WithdrawalMade(amount), null);

        if (!result.isSuccess()) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
