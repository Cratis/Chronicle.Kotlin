```java title="Declarative projection with every-event metadata"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record InventoryRegisteredDeclarativeForEvery(String productName) {
}

@EventType
record InventoryAdjustedDeclarativeForEvery(int quantity) {
}

class InventoryStatusDeclarativeFromEvery {
    public String productName = "";
    public String lastUpdated = "";
}

class InventoryStatusDeclarativeProjection implements IProjectionFor<InventoryStatusDeclarativeFromEvery> {
    @Override
    public void define(IProjectionBuilderFor<InventoryStatusDeclarativeFromEvery> builder) {
        builder
            .from(InventoryRegisteredDeclarativeForEvery.class)
            .from(InventoryAdjustedDeclarativeForEvery.class)
            .fromEvery(every -> {
                every.set("lastUpdated").toEventContextProperty("occurred");
            });
    }
}
```
