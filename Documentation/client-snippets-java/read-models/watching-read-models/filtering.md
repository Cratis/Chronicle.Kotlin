```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import io.cratis.chronicle.java.ReadModelsJavaBridge;
import kotlinx.coroutines.Job;

@ReadModel
class WatchingFilteringOrder {
    private String status = "";
    private double totalAmount = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}

class ReadModelsWatchingReadModelsFiltering {
    // Filtering happens client-side, inside the callback - the server still sends every change
    // for the read model type.
    Job watchHighValueOrders(EventStore store, double threshold) {
        return ReadModelsJavaBridge.watch(store.getReadModels(), WatchingFilteringOrder.class, changeset -> {
            WatchingFilteringOrder order = changeset.getReadModel();
            if (order == null || order.getTotalAmount() <= threshold) {
                return;
            }

            System.out.println(changeset.getModelKey() + ": " + order.getTotalAmount());
        });
    }
}
```
