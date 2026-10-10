```java
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.EventLogJavaBridge;

class AccountService {
    private final IEventLog eventLog;

    AccountService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void withdraw(String accountId, double amount) {
        var result = EventLogJavaBridge.append(eventLog, accountId, new WithdrawalMade(amount), null);

        if (!result.isSuccess()) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
