```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecSetPropsAccountProjection implements IProjectionFor<DecSetPropsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecSetPropsAccount> builder) {
        builder
            .from(DecSetPropsAccountOpened.class, fb -> {
                fb.<String>set("accountNumber").toProperty("number");
                fb.<String>set("customerName").toProperty("owner.name");
                // isActive is not set: the JVM fluent builder in 6.4.0 cannot set a constant value.
                fb.<String>set("openedAt").toProperty("timestamp");
            })
            .from(DecSetPropsMoneyDeposited.class, fb -> {
                fb.<Double>set("balance").toProperty("amount");
                fb.<String>set("lastTransaction").toProperty("timestamp");
            });
    }
}
```
