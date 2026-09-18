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
record MbVariantFullIssueCreated(String title) {}

@EventType
record MbVariantFullPullRequestCreated(String pullRequestUrl) {}

@EventType
record MbVariantFullBuildCompleted(String buildStatus) {}

@EventType
record MbVariantFullTitleChanged(String title) {}

/**
 * Anchors the logical identity shared by MbVariantFullBacklogItem and MbVariantFullPullRequestItem.
 * Deliberately not a read model itself.
 */
class MbVariantFullWorkItem {}

/** The variant an entity is in before a pull request exists for it. */
@ReadModel
@VariantOf(identity = MbVariantFullWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantFullIssueCreated.class)
@FromEvent(eventType = MbVariantFullIssueCreated.class)
class MbVariantFullBacklogItem {
    @FromEventSourceId
    public String id = "";
    public String title = "";
}

/**
 * The variant an entity enters once a pull request is created for it. buildStatus is mapped from
 * MbVariantFullBuildCompleted - an event that is NOT this variant's entering event, so it becomes
 * an update-only join and can never create the row on its own.
 */
@ReadModel
@VariantOf(identity = MbVariantFullWorkItem.class, key = "id")
@EntersOn(eventType = MbVariantFullPullRequestCreated.class)
@FromEvent(eventType = MbVariantFullPullRequestCreated.class)
@FromEvent(eventType = MbVariantFullBuildCompleted.class)
class MbVariantFullPullRequestItem {
    @FromEventSourceId
    public String id = "";
    public String title = "";

    @SetFrom(propertyPath = "pullRequestUrl", eventType = MbVariantFullPullRequestCreated.class)
    public String pullRequestUrl = "";

    @SetFrom(propertyPath = "buildStatus", eventType = MbVariantFullBuildCompleted.class)
    public String buildStatus = "";
}

/** Declares a mapping every variant of MbVariantFullWorkItem shares. */
@GlobalFor(identity = MbVariantFullWorkItem.class)
@FromEvent(eventType = MbVariantFullTitleChanged.class)
class MbVariantFullSharedHandlers {
    @SetFrom(propertyPath = "title", eventType = MbVariantFullTitleChanged.class)
    public String title = "";
}
```
