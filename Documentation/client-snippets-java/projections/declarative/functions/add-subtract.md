```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.math.BigDecimal;

@EventType
record DecFunctionsAccountOpened(String number) {}

@EventType
record DecFunctionsMoneyDeposited(BigDecimal amount) {}

@EventType
record DecFunctionsMoneyWithdrawn(BigDecimal amount) {}

class DecFunctionsAccount {
    public String number = "";
    public BigDecimal balance = BigDecimal.ZERO;
}

class DecFunctionsAccountProjection implements IProjectionFor<DecFunctionsAccount> {
    @Override
    public void define(IProjectionBuilderFor<DecFunctionsAccount> builder) {
        builder
            .autoMap()
            .from(DecFunctionsAccountOpened.class)
            .from(DecFunctionsMoneyDeposited.class, fb -> {
                fb.add("balance").with("amount");
                return null; // Java lambda returning Unit
            })
            .from(DecFunctionsMoneyWithdrawn.class, fb -> {
                fb.subtract("balance").with("amount");
                return null; // Java lambda returning Unit
            });
    }
}
```
