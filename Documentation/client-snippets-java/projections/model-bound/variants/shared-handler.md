```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.EntersOn;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.FromEventSourceId;
import io.cratis.chronicle.projections.GlobalFor;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.VariantOf;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbVariantSharedIssueCreated(String title) {}

@EventType
record MbVariantSharedPullRequestCreated(String pullRequestUrl) {}

@EventType
record MbVariantSharedTitleChanged(String title) {}

class MbVariantSharedWorkItem {}

@ReadModel
@VariantOf(identity = MbVariantSharedWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantSharedIssueCreated.class)
@FromEvent(eventType = MbVariantSharedIssueCreated.class)
class MbVariantSharedBacklogItem {
    @FromEventSourceId
    public String id = "";
    public String title = "";
}

@ReadModel
@VariantOf(identity = MbVariantSharedWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantSharedPullRequestCreated.class)
@FromEvent(eventType = MbVariantSharedPullRequestCreated.class)
class MbVariantSharedPullRequestItem {
    @FromEventSourceId
    public String id = "";
    public String title = "";

    @SetFrom(propertyPath = "pullRequestUrl", eventType = MbVariantSharedPullRequestCreated.class)
    public String pullRequestUrl = "";
}

/**
 * Declares a mapping every variant of MbVariantSharedWorkItem shares. Every variant must have a
 * title member - one that does not is a declaration error, not a silently skipped mapping.
 */
@GlobalFor(identity = MbVariantSharedWorkItem.class)
@FromEvent(eventType = MbVariantSharedTitleChanged.class)
class MbVariantSharedHandlers {
    @SetFrom(propertyPath = "title", eventType = MbVariantSharedTitleChanged.class)
    public String title = "";
}
```
