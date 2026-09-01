```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.readModels.IReadModelReactor;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record AccountFlagged(String accountId) {}

@ReadModel
class ReactingSideEffectsAccount {
    private String id = "";
    private double balance = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class AccountReviewer implements IReadModelReactor {
    // Returning an event from a handler appends it, using the changed instance's key as the
    // event source id by default.
    public AccountFlagged modified(ReactingSideEffectsAccount account) {
        return new AccountFlagged(account.getId());
    }
}
```
