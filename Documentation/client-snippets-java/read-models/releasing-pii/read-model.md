```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record SupportTicketOpened(String customerId, String requesterName) {}

// Release resolves whose encryption key to use by looking for a property named "id",
// case-insensitive - here that is the ticket's own key, which is also the customer it belongs to.
@ReadModel
class ReleasingReadModelSupportTicket {
    private String id = "";
    @Pii
    private String requesterName = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
}
```
