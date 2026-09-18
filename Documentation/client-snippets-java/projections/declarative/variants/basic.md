```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecVariantBacklogItemProjection implements IProjectionFor<DecVariantBacklogItem> {
    @Override
    public void define(IProjectionBuilderFor<DecVariantBacklogItem> builder) {
        builder
            .variantOf(DecVariantWorkItem.class, "id")
            .entersOn(DecVariantIssueCreated.class);
    }
}

class DecVariantPullRequestItemProjection implements IProjectionFor<DecVariantPullRequestItem> {
    @Override
    public void define(IProjectionBuilderFor<DecVariantPullRequestItem> builder) {
        builder
            .variantOf(DecVariantWorkItem.class, "id")
            .entersOn(DecVariantPullRequestCreated.class);
    }
}
```
