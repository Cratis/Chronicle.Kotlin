```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.RemovedWithJoin;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbRemovalJoinClassEmployeeHired(String name) {}

@EventType
record MbRemovalJoinClassCompanyRegistered(String name) {}

@EventType
class MbRemovalJoinClassCompanyDissolved {}

@ReadModel
@FromEvent(eventType = MbRemovalJoinClassEmployeeHired.class)
@RemovedWithJoin(eventType = MbRemovalJoinClassCompanyDissolved.class)
class MbRemovalJoinClassEmployee {
    @SetFrom(propertyPath = "name", eventType = MbRemovalJoinClassEmployeeHired.class)
    public String name = "";

    @Join(eventType = MbRemovalJoinClassCompanyRegistered.class, eventPropertyName = "name")
    public String companyName = "";
}
```
