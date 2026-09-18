```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecVariantFullIssueCreated(String title) {}

@EventType
record DecVariantFullPullRequestCreated(String pullRequestUrl) {}

@EventType
record DecVariantFullBuildCompleted(String buildStatus) {}

/**
 * Anchors the logical identity shared by DecVariantFullBacklogItem and
 * DecVariantFullPullRequestItem. Deliberately not a read model itself.
 */
class DecVariantFullWorkItem {}

class DecVariantFullBacklogItem {
    public String id = "";
    public String title = "";
}

class DecVariantFullPullRequestItem {
    public String id = "";
    public String pullRequestUrl = "";
    public String buildStatus = "";
}

/** The variant an entity is in before a pull request exists for it. */
class DecVariantFullBacklogItemProjection implements IProjectionFor<DecVariantFullBacklogItem> {
    @Override
    public void define(IProjectionBuilderFor<DecVariantFullBacklogItem> builder) {
        builder
            .variantOf(DecVariantFullWorkItem.class, "id")
            .entersOn(DecVariantFullIssueCreated.class);
    }
}

/**
 * The variant an entity enters once a pull request is created for it. buildStatus comes from
 * DecVariantFullBuildCompleted - an event that is NOT this variant's entering event, so the
 * builder reclassifies it into an update-only join and it can never create the row on its own.
 */
class DecVariantFullPullRequestItemProjection implements IProjectionFor<DecVariantFullPullRequestItem> {
    @Override
    public void define(IProjectionBuilderFor<DecVariantFullPullRequestItem> builder) {
        builder
            .variantOf(DecVariantFullWorkItem.class, "id")
            .entersOn(DecVariantFullPullRequestCreated.class)
            .from(DecVariantFullBuildCompleted.class);
    }
}
```
