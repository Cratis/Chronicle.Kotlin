```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecEventContextUserActivityProjection implements IProjectionFor<DecEventContextUserActivity> {
    @Override
    public void define(IProjectionBuilderFor<DecEventContextUserActivity> builder) {
        builder
            .from(DecEventContextUserLoggedIn.class, fb -> {
                fb.set("userId").toEventSourceId();
                fb.set("lastLogin").toEventContextProperty("occurred");
            })
            .from(DecEventContextUserPerformedAction.class, fb -> {
                fb.set("userId").toEventSourceId();
                fb.set("lastActivity").toEventContextProperty("occurred");
            });
    }
}
```
