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
data class MbVariantSharedIssueCreated(val title: String)

@EventType
data class MbVariantSharedPullRequestCreated(val pullRequestUrl: String)

@EventType
data class MbVariantSharedTitleChanged(val title: String)

class MbVariantSharedWorkItem

@ReadModel
@VariantOf(MbVariantSharedWorkItem::class, key = "id")
@EntersOn(MbVariantSharedIssueCreated::class)
@FromEvent(MbVariantSharedIssueCreated::class)
data class MbVariantSharedBacklogItem(
    @FromEventSourceId
    val id: String = "",
    val title: String = ""
)

@ReadModel
@VariantOf(MbVariantSharedWorkItem::class, key = "id")
@EntersOn(MbVariantSharedPullRequestCreated::class)
@FromEvent(MbVariantSharedPullRequestCreated::class)
data class MbVariantSharedPullRequestItem(
    @FromEventSourceId
    val id: String = "",
    val title: String = "",

    @SetFrom("pullRequestUrl", MbVariantSharedPullRequestCreated::class)
    val pullRequestUrl: String = ""
)

/**
 * Declares a mapping every variant of MbVariantSharedWorkItem shares. Every variant must have a
 * title member - one that does not is a declaration error, not a silently skipped mapping.
 */
@GlobalFor(MbVariantSharedWorkItem::class)
@FromEvent(MbVariantSharedTitleChanged::class)
data class MbVariantSharedHandlers(
    @SetFrom("title", MbVariantSharedTitleChanged::class)
    val title: String = ""
)
```
