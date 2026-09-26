```java
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecSetPropsAccountProjection implements IProjectionFor<DecSetPropsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecSetPropsAccount> builder) {
        builder
            .from(DecSetPropsAccountOpened.class, fb -> {
                fb.<String>set("accountNumber").toProperty("number");
                fb.<String>set("customerName").toProperty("owner.name");
                fb.<Boolean>set("isActive").toValue(true);
                fb.<String>set("openedAt").toProperty("timestamp");
            })
            .from(DecSetPropsMoneyDeposited.class, fb -> {
                fb.<Double>set("balance").toProperty("amount");
                fb.<String>set("lastTransaction").toProperty("timestamp");
            });
    }
}
```
