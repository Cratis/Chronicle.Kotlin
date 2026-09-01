```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-functions-item-added")
record DecFunctionsItemAdded(String name) {}

@EventType(id = "dec-functions-item-removed")
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
                return null; // Java lambda returning Unit
            })
            .from(DecFunctionsItemRemoved.class, fb -> {
                fb.decrement("quantity");
                return null; // Java lambda returning Unit
            });
    }
}
```
