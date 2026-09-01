```java title="Map context fields with FromAll"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AccountTouchedDeclarativeAll(String reason) {}

class AccountAuditDeclarativeAll {
    public String lastUpdated = "";
    public String lastEventSequence = "";
    public String lastCorrelationId = "";
}

class AccountAuditDeclarativeAllProjection implements IProjectionFor<AccountAuditDeclarativeAll> {
    @Override
    public void define(IProjectionBuilderFor<AccountAuditDeclarativeAll> builder) {
        builder
            .from(AccountTouchedDeclarativeAll.class)
            .fromAll(feb -> {
                feb.set("lastUpdated").toEventContextProperty("occurred");
                feb.set("lastEventSequence").toEventContextProperty("sequenceNumber");
                feb.set("lastCorrelationId").toEventContextProperty("correlationId");
                return null; // Java lambda returning Unit
            });
    }
}
```
