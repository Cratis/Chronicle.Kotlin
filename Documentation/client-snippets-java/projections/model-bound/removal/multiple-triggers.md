```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.RemovedWithJoin;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbRemovalMultipleAccountOpened(String name) {}

@EventType
class MbRemovalMultipleAccountClosed {}

@EventType
record MbRemovalMultipleAccountMerged(String sourceAccountId) {}

@EventType
class MbRemovalMultipleOrganizationClosed {}

@ReadModel
@FromEvent(eventType = MbRemovalMultipleAccountOpened.class)
@RemovedWith(eventType = MbRemovalMultipleAccountClosed.class)
@RemovedWith(eventType = MbRemovalMultipleAccountMerged.class, key = "sourceAccountId")
@RemovedWithJoin(eventType = MbRemovalMultipleOrganizationClosed.class)
class MbRemovalMultipleAccount {
    @SetFrom(propertyPath = "name", eventType = MbRemovalMultipleAccountOpened.class)
    public String name = "";
}
```
