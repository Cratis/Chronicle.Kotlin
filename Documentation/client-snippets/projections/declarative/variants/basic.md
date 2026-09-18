```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecVariantBacklogItemProjection : IProjectionFor<DecVariantBacklogItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantBacklogItem>) {
        builder
            .variantOf(DecVariantWorkItem::class, DecVariantBacklogItem::id)
            .entersOn(DecVariantIssueCreated::class)
    }
}

class DecVariantPullRequestItemProjection : IProjectionFor<DecVariantPullRequestItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantPullRequestItem>) {
        builder
            .variantOf(DecVariantWorkItem::class, DecVariantPullRequestItem::id)
            .entersOn(DecVariantPullRequestCreated::class)
    }
}
```
