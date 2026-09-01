```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class GettingCollectionFilteringAccount {
    private String name = "";
    private double balance = 0;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class ReadModelsGettingCollectionInstancesFiltering {
    // The read returns every instance; apply language-native filtering afterwards.
    List<GettingCollectionFilteringAccount> highValueAccounts(EventStore store, double threshold) {
        List<GettingCollectionFilteringAccount> accounts = ReadModelsJavaBridge.getInstances(store.getReadModels(), GettingCollectionFilteringAccount.class);
        return accounts.stream()
            .filter(account -> account.getBalance() > threshold)
            .sorted(Comparator.comparingDouble(GettingCollectionFilteringAccount::getBalance).reversed())
            .collect(Collectors.toList());
    }
}
```
