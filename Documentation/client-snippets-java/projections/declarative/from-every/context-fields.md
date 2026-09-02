```java title="Map multiple context fields"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AccountTouchedDeclarativeEvery(String reason) {}

class AccountAuditDeclarativeEvery {
    public String lastUpdated = "";
    public String lastEventSequence = "";
    public String lastCorrelationId = "";
}

class AccountAuditDeclarativeEveryProjection implements IProjectionFor<AccountAuditDeclarativeEvery> {
    @Override
    public void define(IProjectionBuilderFor<AccountAuditDeclarativeEvery> builder) {
        builder
            .from(AccountTouchedDeclarativeEvery.class)
            .fromEvery(feb -> {
                feb.set("lastUpdated").toEventContextProperty("occurred");
                feb.set("lastEventSequence").toEventContextProperty("sequenceNumber");
                feb.set("lastCorrelationId").toEventContextProperty("correlationId");
            });
    }
}
```
