```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.readModels.ReadModelSnapshot;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class SnapshotsBasicOrder {
    private String status = "";
    private double total = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}

class ReadModelsGettingSnapshotsBasic {
    void countSnapshots(EventStore store, String orderId) {
        List<ReadModelSnapshot<SnapshotsBasicOrder>> snapshots = ReadModelsJavaBridge.getSnapshotsById(store.getReadModels(), SnapshotsBasicOrder.class, orderId);
        System.out.println("Found " + snapshots.size() + " snapshots.");
    }
}
```
