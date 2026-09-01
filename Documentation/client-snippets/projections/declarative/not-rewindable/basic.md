```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecNotRewindableAuditLogProjection : IProjectionFor<DecNotRewindableAuditLogEntry> {
    override fun define(builder: IProjectionBuilderFor<DecNotRewindableAuditLogEntry>) {
        builder
            .notRewindable()
            .fromEvery { it.set(DecNotRewindableAuditLogEntry::processedAt).toEventContextProperty("occurred") }
            .from(DecNotRewindableUserAction::class)
    }
}
```
