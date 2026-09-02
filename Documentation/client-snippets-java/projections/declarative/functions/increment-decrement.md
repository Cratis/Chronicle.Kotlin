```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecFunctionsItemAdded(String name) {}

@EventType
record DecFunctionsItemRemoved(String name) {}

class DecFunctionsInventory {
    public int quantity = 0;
}

class DecFunctionsInventoryProjection implements IProjectionFor<DecFunctionsInventory> {
    @Override
    public void define(IProjectionBuilderFor<DecFunctionsInventory> builder) {
        builder
            .autoMap()
            .from(DecFunctionsItemAdded.class, fb -> {
                fb.increment("quantity");
            })
            .from(DecFunctionsItemRemoved.class, fb -> {
                fb.decrement("quantity");
            });
    }
}
```
