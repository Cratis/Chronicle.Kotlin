```java
import io.cratis.chronicle.readModels.IReadModelReactor;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

@ReadModel
class ReactingCollectionAccount {
    private String name = "";
    private double balance = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class AccountBatchProjector implements IReadModelReactor {
    public void modified(List<ReactingCollectionAccount> accounts) {
        accounts.forEach(this::sync);
    }

    private void sync(ReactingCollectionAccount account) { }
}
```
