```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.readModels.ReadModelSnapshot;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class SnapshotAccountInfo {
    private String name = "";
    private double balance = 0.0;

    public SnapshotAccountInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

class ReadModelsSnapshotsBasic {
    // Gets the full history of intermediate states for a read model instance, grouped by
    // correlation id — unlike getInstanceByKey, which only returns the latest state.
    void getAccountSnapshotHistory(EventStore store, String accountId) {
        List<ReadModelSnapshot<SnapshotAccountInfo>> snapshots =
            ReadModelsJavaBridge.getSnapshotsById(store.getReadModels(), SnapshotAccountInfo.class, accountId);
        for (ReadModelSnapshot<SnapshotAccountInfo> snapshot : snapshots) {
            System.out.println(snapshot.getOccurred() + ": " + snapshot.getInstance().getName() +
                " -> " + snapshot.getInstance().getBalance());
        }
    }
}
```
