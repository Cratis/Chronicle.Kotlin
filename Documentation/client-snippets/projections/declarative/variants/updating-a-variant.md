```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecVariantUpdatingPullRequestCreated(val pullRequestUrl: String)

@EventType
data class DecVariantUpdatingBuildCompleted(val buildStatus: String)

class DecVariantUpdatingWorkItem

data class DecVariantUpdatingPullRequestItem(val id: String = "", val pullRequestUrl: String = "", val buildStatus: String = "")

/**
 * from(DecVariantUpdatingBuildCompleted::class) is declared exactly like an ordinary multi-event
 * projection. Because that event is NOT the one named with entersOn, the builder automatically
 * reclassifies it into an update-only join on the variant's own key when the definition is built -
 * it can bring an already-active instance up to date, but it can never create one on its own.
 */
class DecVariantUpdatingPullRequestItemProjection : IProjectionFor<DecVariantUpdatingPullRequestItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantUpdatingPullRequestItem>) {
        builder
            .variantOf(DecVariantUpdatingWorkItem::class, DecVariantUpdatingPullRequestItem::id)
            .entersOn(DecVariantUpdatingPullRequestCreated::class)
            .from(DecVariantUpdatingBuildCompleted::class)
    }
}
```
