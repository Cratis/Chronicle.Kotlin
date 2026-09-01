```java title="Map the event source id with FromAll"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "account-opened-declarative-all")
record AccountOpenedDeclarativeAll(String ownerName) {}

class AccountSummaryDeclarativeAll {
    public String accountId = "";
    public String ownerName = "";
}

class AccountSummaryDeclarativeAllProjection implements IProjectionFor<AccountSummaryDeclarativeAll> {
    @Override
    public void define(IProjectionBuilderFor<AccountSummaryDeclarativeAll> builder) {
        builder
            .from(AccountOpenedDeclarativeAll.class)
            .fromAll(feb -> {
                feb.set("accountId").toEventSourceId();
                return null; // Java lambda returning Unit
            });
    }
}
```
