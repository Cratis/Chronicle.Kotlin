```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-removal-with-key-account-opened")
record MbRemovalWithKeyAccountOpened(String name) {}

@EventType(id = "mb-removal-with-key-account-closed")
record MbRemovalWithKeyAccountClosed(String accountId) {}

@ReadModel
@FromEvent(eventType = MbRemovalWithKeyAccountOpened.class)
@RemovedWith(eventType = MbRemovalWithKeyAccountClosed.class, key = "accountId")
class MbRemovalWithKeyAccount {
    @SetFrom(propertyPath = "name", eventType = MbRemovalWithKeyAccountOpened.class)
    public String name = "";
}
```
