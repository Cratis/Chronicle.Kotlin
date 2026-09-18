```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EntersOn
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEventSourceId
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.VariantOf
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbVariantUpdatingPullRequestCreated(val pullRequestUrl: String)

@EventType
data class MbVariantUpdatingBuildCompleted(val buildStatus: String)

class MbVariantUpdatingWorkItem

/**
 * buildStatus is mapped from MbVariantUpdatingBuildCompleted - an event that is NOT this variant's
 * entering event, so it is automatically reclassified into an update-only join. It can bring an
 * already-active instance up to date, but it can never create one on its own.
 */
@ReadModel
@VariantOf(MbVariantUpdatingWorkItem::class, key = "id")
@EntersOn(MbVariantUpdatingPullRequestCreated::class)
@FromEvent(MbVariantUpdatingPullRequestCreated::class)
@FromEvent(MbVariantUpdatingBuildCompleted::class)
data class MbVariantUpdatingPullRequestItem(
    @FromEventSourceId
    val id: String = "",

    @SetFrom("pullRequestUrl", MbVariantUpdatingPullRequestCreated::class)
    val pullRequestUrl: String = "",

    @SetFrom("buildStatus", MbVariantUpdatingBuildCompleted::class)
    val buildStatus: String = ""
)
```
