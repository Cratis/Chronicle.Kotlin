```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EntersOn
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEventSourceId
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.VariantOf
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbVariantIssueCreated(val title: String)

@EventType
data class MbVariantPullRequestCreated(val pullRequestUrl: String)

/**
 * Anchors the logical identity shared by every variant. It does not need to be a read model
 * itself, and it does not need a common supertype with any of the variants.
 */
class MbVariantWorkItem

@ReadModel
@VariantOf(MbVariantWorkItem::class, key = "id")
@EntersOn(MbVariantIssueCreated::class)
@FromEvent(MbVariantIssueCreated::class)
data class MbVariantBacklogItem(
    @FromEventSourceId
    val id: String = "",

    @SetFrom("title", MbVariantIssueCreated::class)
    val title: String = ""
)

@ReadModel
@VariantOf(MbVariantWorkItem::class, key = "id")
@EntersOn(MbVariantPullRequestCreated::class)
@FromEvent(MbVariantPullRequestCreated::class)
data class MbVariantPullRequestItem(
    @FromEventSourceId
    val id: String = "",

    @SetFrom("pullRequestUrl", MbVariantPullRequestCreated::class)
    val pullRequestUrl: String = ""
)
```
