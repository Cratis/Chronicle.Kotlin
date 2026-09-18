```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecVariantFullIssueCreated(val title: String)

@EventType
data class DecVariantFullPullRequestCreated(val pullRequestUrl: String)

@EventType
data class DecVariantFullBuildCompleted(val buildStatus: String)

/**
 * Anchors the logical identity shared by DecVariantFullBacklogItem and
 * DecVariantFullPullRequestItem. Deliberately not a read model itself.
 */
class DecVariantFullWorkItem

data class DecVariantFullBacklogItem(val id: String = "", val title: String = "")

data class DecVariantFullPullRequestItem(val id: String = "", val pullRequestUrl: String = "", val buildStatus: String = "")

/** The variant an entity is in before a pull request exists for it. */
class DecVariantFullBacklogItemProjection : IProjectionFor<DecVariantFullBacklogItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantFullBacklogItem>) {
        builder
            .variantOf(DecVariantFullWorkItem::class, DecVariantFullBacklogItem::id)
            .entersOn(DecVariantFullIssueCreated::class)
    }
}

/**
 * The variant an entity enters once a pull request is created for it. buildStatus comes from
 * DecVariantFullBuildCompleted - an event that is NOT this variant's entering event, so the
 * builder reclassifies it into an update-only join and it can never create the row on its own.
 */
class DecVariantFullPullRequestItemProjection : IProjectionFor<DecVariantFullPullRequestItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantFullPullRequestItem>) {
        builder
            .variantOf(DecVariantFullWorkItem::class, DecVariantFullPullRequestItem::id)
            .entersOn(DecVariantFullPullRequestCreated::class)
            .from(DecVariantFullBuildCompleted::class)
    }
}
```
