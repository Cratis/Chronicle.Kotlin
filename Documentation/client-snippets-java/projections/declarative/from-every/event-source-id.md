```java title="Map the event source id"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "account-opened-declarative-every")
record AccountOpenedDeclarativeEvery(String ownerName) {}

class AccountSummaryDeclarativeEvery {
    public String accountId = "";
    public String ownerName = "";
}

class AccountSummaryDeclarativeEveryProjection implements IProjectionFor<AccountSummaryDeclarativeEvery> {
    @Override
    public void define(IProjectionBuilderFor<AccountSummaryDeclarativeEvery> builder) {
        builder
            .from(AccountOpenedDeclarativeEvery.class)
            .fromEvery(feb -> {
                feb.set("accountId").toEventSourceId();
                return null; // Java lambda returning Unit
            });
    }
}
```
