```java
import io.cratis.chronicle.readModels.IReadModelReactor;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
class ReactingReactorAccount {
    private String name = "";
    private double balance = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class AccountNotifier implements IReadModelReactor {
    public void added(ReactingReactorAccount account) { sendWelcome(account); }
    public void modified(ReactingReactorAccount account) { sendUpdated(account); }
    public void removed(ReactingReactorAccount account) { sendClosed(account); }

    private void sendWelcome(ReactingReactorAccount account) { }
    private void sendUpdated(ReactingReactorAccount account) { }
    private void sendClosed(ReactingReactorAccount account) { }
}
```
