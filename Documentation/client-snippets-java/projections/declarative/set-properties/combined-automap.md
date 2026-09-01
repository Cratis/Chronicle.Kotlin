```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecSetPropsCombinedAccountProjection implements IProjectionFor<DecSetPropsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecSetPropsAccount> builder) {
        builder
            // AutoMap is on by default; these two properties don't have matching event
            // properties, so they're set explicitly and everything else is left to AutoMap.
            .from(DecSetPropsAccountOpened.class, fb -> {
                fb.<String>set("customerName").to(e -> e.owner().name());
                fb.<Boolean>set("isActive").to(e -> true);
                return null; // Java lambda returning Unit
            })
            .from(DecSetPropsMoneyDeposited.class); // Uses AutoMap for all properties
    }
}
```
