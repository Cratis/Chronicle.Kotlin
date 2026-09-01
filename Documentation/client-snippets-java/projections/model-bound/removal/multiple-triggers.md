```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.RemovedWithJoin;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-removal-multiple-account-opened")
record MbRemovalMultipleAccountOpened(String name) {}

@EventType(id = "mb-removal-multiple-account-closed")
class MbRemovalMultipleAccountClosed {}

@EventType(id = "mb-removal-multiple-account-merged")
record MbRemovalMultipleAccountMerged(String sourceAccountId) {}

@EventType(id = "mb-removal-multiple-organization-closed")
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
