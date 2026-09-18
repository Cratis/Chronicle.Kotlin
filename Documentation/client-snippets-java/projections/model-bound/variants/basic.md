```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.EntersOn;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEventSourceId;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.VariantOf;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbVariantIssueCreated(String title) {}

@EventType
record MbVariantPullRequestCreated(String pullRequestUrl) {}

/**
 * Anchors the logical identity shared by every variant. It does not need to be a read model
 * itself, and it does not need a common supertype with any of the variants.
 */
class MbVariantWorkItem {}

@ReadModel
@VariantOf(identity = MbVariantWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantIssueCreated.class)
@FromEvent(eventType = MbVariantIssueCreated.class)
class MbVariantBacklogItem {
    @FromEventSourceId
    public String id = "";

    @SetFrom(propertyPath = "title", eventType = MbVariantIssueCreated.class)
    public String title = "";
}

@ReadModel
@VariantOf(identity = MbVariantWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantPullRequestCreated.class)
@FromEvent(eventType = MbVariantPullRequestCreated.class)
class MbVariantPullRequestItem {
    @FromEventSourceId
    public String id = "";

    @SetFrom(propertyPath = "pullRequestUrl", eventType = MbVariantPullRequestCreated.class)
    public String pullRequestUrl = "";
}
```
