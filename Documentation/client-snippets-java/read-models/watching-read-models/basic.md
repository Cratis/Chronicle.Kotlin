```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import io.cratis.chronicle.java.ReadModelsJavaBridge;
import kotlinx.coroutines.Job;

@ReadModel
class WatchingBasicOrder {
    private String status = "";
    private double totalAmount = 0;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}

class ReadModelsWatchingReadModelsBasic {
    Job watchOrders(EventStore store) {
        return ReadModelsJavaBridge.watch(store.getReadModels(), WatchingBasicOrder.class, changeset -> {
            if (changeset.getRemoved() || changeset.getReadModel() == null) {
                return;
            }

            System.out.println(changeset.getModelKey() + ": " + changeset.getReadModel().getStatus());
        });
    }
}
```
