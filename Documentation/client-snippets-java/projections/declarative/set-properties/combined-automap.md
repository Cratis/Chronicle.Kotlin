```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecSetPropsCombinedAccountProjection implements IProjectionFor<DecSetPropsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecSetPropsAccount> builder) {
        builder
            // AutoMap is on by default for matching event properties.
            .from(DecSetPropsAccountOpened.class, fb -> {
                // Map the property AutoMap cannot find: it is nested on the event.
                fb.<String>set("customerName").toProperty("owner.name");
                // isActive is not set: the JVM fluent builder in 6.4.0 cannot set a constant value.
            })
            .from(DecSetPropsMoneyDeposited.class); // Uses AutoMap for all properties
    }
}
```
