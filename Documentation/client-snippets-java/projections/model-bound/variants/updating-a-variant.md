```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.EntersOn;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEventSourceId;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.VariantOf;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbVariantUpdatingPullRequestCreated(String pullRequestUrl) {}

@EventType
record MbVariantUpdatingBuildCompleted(String buildStatus) {}

class MbVariantUpdatingWorkItem {}

/**
 * buildStatus is mapped from MbVariantUpdatingBuildCompleted - an event that is NOT this variant's
 * entering event, so it is automatically reclassified into an update-only join. It can bring an
 * already-active instance up to date, but it can never create one on its own.
 */
@ReadModel
@VariantOf(identity = MbVariantUpdatingWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantUpdatingPullRequestCreated.class)
@FromEvent(eventType = MbVariantUpdatingPullRequestCreated.class)
@FromEvent(eventType = MbVariantUpdatingBuildCompleted.class)
class MbVariantUpdatingPullRequestItem {
    @FromEventSourceId
    public String id = "";

    @SetFrom(propertyPath = "pullRequestUrl", eventType = MbVariantUpdatingPullRequestCreated.class)
    public String pullRequestUrl = "";

    @SetFrom(propertyPath = "buildStatus", eventType = MbVariantUpdatingBuildCompleted.class)
    public String buildStatus = "";
}
```
