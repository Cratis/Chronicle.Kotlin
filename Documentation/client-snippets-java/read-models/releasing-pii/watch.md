```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.readModels.ReadModel;

import io.cratis.chronicle.java.ReadModelsJavaBridge;
import kotlinx.coroutines.Job;

@ReadModel
class ReleasingWatchSupportTicket {
    private String id = "";
    private String requesterName = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
}

class ReadModelsReleasingPiiWatch {
    // watch() is the one built-in read that does not release PII automatically - release each
    // change yourself as it arrives.
    Job watchTickets(EventStore store) {
        return ReadModelsJavaBridge.watch(store.getReadModels(), ReleasingWatchSupportTicket.class, changeset -> {
            if (changeset.getRemoved() || changeset.getReadModel() == null) {
                return;
            }

            ReleasingWatchSupportTicket ticket = ReadModelsJavaBridge.release(store.getReadModels(), changeset.getReadModel());
            System.out.println(changeset.getModelKey() + ": " + ticket.getRequesterName());
        });
    }
}
```
