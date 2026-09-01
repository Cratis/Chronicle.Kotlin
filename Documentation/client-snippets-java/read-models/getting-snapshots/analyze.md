```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.readModels.ReadModelSnapshot;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class SnapshotsAnalyzeOrder {
    private String status = "";
    private double total = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}

class ReadModelsGettingSnapshotsAnalyze {
    void analyzeSnapshots(EventStore store, String orderId) {
        List<ReadModelSnapshot<SnapshotsAnalyzeOrder>> snapshots = ReadModelsJavaBridge.getSnapshotsById(store.getReadModels(), SnapshotsAnalyzeOrder.class, orderId);
        for (ReadModelSnapshot<SnapshotsAnalyzeOrder> snapshot : snapshots) {
            System.out.println("Snapshot at " + snapshot.getOccurred() + ":");
            System.out.println("  Correlation ID: " + snapshot.getCorrelationId());
            System.out.println("  Event count: " + snapshot.getEvents().size());
            System.out.println("  State: " + snapshot.getInstance());
        }
    }
}
```
