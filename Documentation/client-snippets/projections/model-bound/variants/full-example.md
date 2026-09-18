```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EntersOn
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEventSourceId
import io.cratis.chronicle.projections.GlobalFor
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.VariantOf
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbVariantFullIssueCreated(val title: String)

@EventType
data class MbVariantFullPullRequestCreated(val pullRequestUrl: String)

@EventType
data class MbVariantFullBuildCompleted(val buildStatus: String)

@EventType
data class MbVariantFullTitleChanged(val title: String)

/**
 * Anchors the logical identity shared by MbVariantFullBacklogItem and MbVariantFullPullRequestItem.
 * Deliberately not a read model itself.
 */
class MbVariantFullWorkItem

/** The variant an entity is in before a pull request exists for it. */
@ReadModel
@VariantOf(MbVariantFullWorkItem::class, key = "id")
@EntersOn(MbVariantFullIssueCreated::class)
@FromEvent(MbVariantFullIssueCreated::class)
data class MbVariantFullBacklogItem(
    @FromEventSourceId
    val id: String = "",
    val title: String = ""
)

/**
 * The variant an entity enters once a pull request is created for it. buildStatus is mapped from
 * MbVariantFullBuildCompleted - an event that is NOT this variant's entering event, so it becomes
 * an update-only join and can never create the row on its own.
 */
@ReadModel
@VariantOf(MbVariantFullWorkItem::class, key = "id")
@EntersOn(MbVariantFullPullRequestCreated::class)
@FromEvent(MbVariantFullPullRequestCreated::class)
@FromEvent(MbVariantFullBuildCompleted::class)
data class MbVariantFullPullRequestItem(
    @FromEventSourceId
    val id: String = "",
    val title: String = "",

    @SetFrom("pullRequestUrl", MbVariantFullPullRequestCreated::class)
    val pullRequestUrl: String = "",

    @SetFrom("buildStatus", MbVariantFullBuildCompleted::class)
    val buildStatus: String = ""
)

/** Declares a mapping every variant of MbVariantFullWorkItem shares. */
@GlobalFor(MbVariantFullWorkItem::class)
@FromEvent(MbVariantFullTitleChanged::class)
data class MbVariantFullSharedHandlers(
    @SetFrom("title", MbVariantFullTitleChanged::class)
    val title: String = ""
)
```
