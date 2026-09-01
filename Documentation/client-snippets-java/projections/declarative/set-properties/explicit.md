```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.math.BigDecimal;

class DecSetPropsAccountProjection implements IProjectionFor<DecSetPropsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecSetPropsAccount> builder) {
        builder
            .from(DecSetPropsAccountOpened.class, fb -> {
                fb.<String>set("accountNumber").to(e -> e.number());
                fb.<String>set("customerName").to(e -> e.owner().name());
                fb.<BigDecimal>set("balance").to(e -> new BigDecimal("42.0"));
                fb.<Boolean>set("isActive").to(e -> true);
                fb.<String>set("openedAt").to(e -> e.timestamp());
                return null; // Java lambda returning Unit
            })
            .from(DecSetPropsMoneyDeposited.class, fb -> {
                fb.<Double>set("balance").to(e -> e.amount());
                fb.<String>set("lastTransaction").to(e -> e.timestamp());
                return null; // Java lambda returning Unit
            });
    }
}
```
