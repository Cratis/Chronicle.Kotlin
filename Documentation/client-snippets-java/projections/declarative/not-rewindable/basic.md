```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecNotRewindableAuditLogProjection implements IProjectionFor<DecNotRewindableAuditLogEntry> {
    @Override
    public void define(IProjectionBuilderFor<DecNotRewindableAuditLogEntry> builder) {
        builder
            .notRewindable()
            .fromEvery(feb -> {
                feb.set("processedAt").toEventContextProperty("occurred");
            })
            .from(DecNotRewindableUserAction.class);
    }
}
```
