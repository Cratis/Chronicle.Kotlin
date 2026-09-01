```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class GettingCollectionAccount {
    private String name = "";
    private double balance = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class ReadModelsGettingCollectionInstancesBasic {
    void printAllAccounts(EventStore store) {
        List<GettingCollectionAccount> accounts = ReadModelsJavaBridge.getInstances(store.getReadModels(), GettingCollectionAccount.class);
        accounts.forEach(account -> System.out.println(account.getName() + ": " + account.getBalance()));
    }
}
```
