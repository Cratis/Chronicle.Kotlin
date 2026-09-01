```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class ReleasingSingleInstanceSupportTicket {
    private String id = "";
    private String requesterName = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
}

class ReadModelsReleasingPiiSingleInstance {
    ReleasingSingleInstanceSupportTicket release(EventStore store, ReleasingSingleInstanceSupportTicket ticket) {
        return ReadModelsJavaBridge.release(store.getReadModels(), ticket);
    }
}
```
