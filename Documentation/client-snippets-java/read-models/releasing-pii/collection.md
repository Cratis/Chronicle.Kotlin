```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.List;

import io.cratis.chronicle.java.ReadModelsJavaBridge;

@ReadModel
class ReleasingCollectionSupportTicket {
    private String id = "";
    private String requesterName = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
}

class ReadModelsReleasingPiiCollection {
    // Releases more than one instance at once - the subject for each is resolved independently.
    List<ReleasingCollectionSupportTicket> releaseAll(EventStore store, List<ReleasingCollectionSupportTicket> tickets) {
        return ReadModelsJavaBridge.releaseMany(store.getReadModels(), tickets);
    }
}
```
