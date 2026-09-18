```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecVariantUpdatingPullRequestCreated(String pullRequestUrl) {}

@EventType
record DecVariantUpdatingBuildCompleted(String buildStatus) {}

class DecVariantUpdatingWorkItem {}

class DecVariantUpdatingPullRequestItem {
    public String id = "";
    public String pullRequestUrl = "";
    public String buildStatus = "";
}

/**
 * from(DecVariantUpdatingBuildCompleted.class) is declared exactly like an ordinary multi-event
 * projection. Because that event is NOT the one named with entersOn, the builder automatically
 * reclassifies it into an update-only join on the variant's own key when the definition is built -
 * it can bring an already-active instance up to date, but it can never create one on its own.
 */
class DecVariantUpdatingPullRequestItemProjection implements IProjectionFor<DecVariantUpdatingPullRequestItem> {
    @Override
    public void define(IProjectionBuilderFor<DecVariantUpdatingPullRequestItem> builder) {
        builder
            .variantOf(DecVariantUpdatingWorkItem.class, "id")
            .entersOn(DecVariantUpdatingPullRequestCreated.class)
            .from(DecVariantUpdatingBuildCompleted.class);
    }
}
```
