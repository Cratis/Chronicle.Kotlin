```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-not-rewindable-user-login-attempt")
record DecNotRewindableUserLoginAttempt(String userId, boolean succeeded) {}

@EventType(id = "dec-not-rewindable-permission-change")
record DecNotRewindablePermissionChange(String userId, String permission) {}

class DecNotRewindableSecurityAuditEntry {
    public String auditedAt = "";
    public long sequenceNumber = 0;
}

class DecNotRewindableSecurityAuditProjection implements IProjectionFor<DecNotRewindableSecurityAuditEntry> {
    @Override
    public void define(IProjectionBuilderFor<DecNotRewindableSecurityAuditEntry> builder) {
        builder
            .notRewindable()
            .fromEvery(feb -> {
                feb.set("auditedAt").toEventContextProperty("occurred");
                feb.set("sequenceNumber").toEventContextProperty("sequenceNumber");
                return null; // Java lambda returning Unit
            })
            .from(DecNotRewindableUserLoginAttempt.class)
            .from(DecNotRewindablePermissionChange.class);
    }
}
```
