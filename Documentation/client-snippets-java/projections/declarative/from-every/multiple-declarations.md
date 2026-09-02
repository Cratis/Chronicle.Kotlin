```java title="Multiple FromEvery declarations"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record UserChangedDeclarativeEveryMultiple(String name) {}

class UserAuditDeclarativeEveryMultiple {
    public String name = "";
    public String lastUpdated = "";
    public String modifiedBy = "";
}

class UserAuditDeclarativeEveryMultipleProjection implements IProjectionFor<UserAuditDeclarativeEveryMultiple> {
    @Override
    public void define(IProjectionBuilderFor<UserAuditDeclarativeEveryMultiple> builder) {
        builder
            .from(UserChangedDeclarativeEveryMultiple.class)
            .fromEvery(feb -> {
                feb.set("lastUpdated").toEventContextProperty("occurred");
            })
            .fromEvery(feb -> {
                feb.set("modifiedBy").toEventContextProperty("causedBy");
            });
    }
}
```
