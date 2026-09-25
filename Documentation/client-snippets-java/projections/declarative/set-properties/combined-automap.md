```java
// Requires io.cratis:chronicle 6.5.0 or later.
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
                fb.<Boolean>set("isActive").toValue(true);
            })
            .from(DecSetPropsMoneyDeposited.class); // Uses AutoMap for all properties
    }
}
```
