```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import io.cratis.chronicle.java.ReadModelsJavaBridge;
import kotlinx.coroutines.Job;

@ReadModel
class WatchAccountInfo {
    private String name = "";
    private double balance = 0.0;

    public WatchAccountInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class ProjectionsEventualConsistencyWatch {
    // Cancel the returned job when observation ends.
    Job watchAccountChanges(EventStore store) {
        return ReadModelsJavaBridge.watch(store.getReadModels(), WatchAccountInfo.class, changeset -> {
            WatchAccountInfo model = changeset.getReadModel();
            String label = changeset.getRemoved() || model == null
                ? "removed"
                : model.getName() + ": " + model.getBalance();
            System.out.println(changeset.getModelKey() + " " + changeset.getChangeType() + ": " + label);
        }, error -> System.err.println("Watch failed: " + error));
    }
}
```
